package com.sharebill.service;

import com.sharebill.core.DebtSimplifier;
import com.sharebill.core.SplitCalculator;
import com.sharebill.core.Transfer;
import com.sharebill.model.BillGroup;
import com.sharebill.model.Expense;
import com.sharebill.model.Member;
import com.sharebill.model.Payment;
import com.sharebill.model.Portion;
import com.sharebill.repository.BillGroupRepository;
import com.sharebill.repository.ExpenseRepository;
import com.sharebill.repository.MemberRepository;
import com.sharebill.repository.PaymentRepository;
import com.sharebill.web.ApiDtos.BalanceView;
import com.sharebill.web.ApiDtos.CreateExpenseRequest;
import com.sharebill.web.ApiDtos.CreateGroupRequest;
import com.sharebill.web.ApiDtos.CreatePaymentRequest;
import com.sharebill.web.ApiDtos.ExpenseView;
import com.sharebill.web.ApiDtos.GroupView;
import com.sharebill.web.ApiDtos.MemberView;
import com.sharebill.web.ApiDtos.PayerInput;
import com.sharebill.web.ApiDtos.PaymentView;
import com.sharebill.web.ApiDtos.PortionView;
import com.sharebill.web.ApiDtos.SplitInput;
import com.sharebill.web.ApiDtos.TransferView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {

    private final BillGroupRepository groups;
    private final MemberRepository members;
    private final ExpenseRepository expenses;
    private final PaymentRepository payments;

    public GroupService(BillGroupRepository groups, MemberRepository members,
                        ExpenseRepository expenses, PaymentRepository payments) {
        this.groups = groups;
        this.members = members;
        this.expenses = expenses;
        this.payments = payments;
    }

    // ---------------- Nhóm & thành viên ----------------

    public GroupView createGroup(CreateGroupRequest req) {
        BillGroup group = groups.save(new BillGroup(req.name().trim()));
        if (req.members() != null) {
            for (String name : req.members()) {
                members.save(new Member(group, name.trim()));
            }
        }
        return toView(group);
    }

    @Transactional(readOnly = true)
    public List<GroupView> listGroups() {
        return groups.findAll().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public GroupView getGroup(Long groupId) {
        return toView(findGroup(groupId));
    }

    public void deleteGroup(Long groupId) {
        BillGroup group = findGroup(groupId);
        payments.deleteAll(payments.findByGroupIdOrderByCreatedAtDesc(groupId));
        expenses.deleteAll(expenses.findByGroupIdOrderByCreatedAtDesc(groupId));
        members.deleteAll(members.findByGroupIdOrderById(groupId));
        groups.delete(group);
    }

    public MemberView addMember(Long groupId, String name) {
        Member m = members.save(new Member(findGroup(groupId), name.trim()));
        return new MemberView(m.getId(), m.getName());
    }

    public void removeMember(Long groupId, Long memberId) {
        findGroup(groupId);
        Member member = memberMap(groupId).get(memberId);
        if (member == null) {
            throw new NotFoundException("Không tìm thấy thành viên " + memberId + " trong nhóm");
        }
        boolean used = expenses.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .anyMatch(e -> involves(e.getPayers(), memberId) || involves(e.getShares(), memberId))
                || payments.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .anyMatch(p -> p.getFromMemberId().equals(memberId) || p.getToMemberId().equals(memberId));
        if (used) {
            throw new IllegalArgumentException("Không thể xóa " + member.getName()
                    + " vì đã có khoản chi hoặc thanh toán liên quan");
        }
        members.delete(member);
    }

    // ---------------- Khoản chi ----------------

    public ExpenseView addExpense(Long groupId, CreateExpenseRequest req) {
        BillGroup group = findGroup(groupId);
        Map<Long, Member> memberById = memberMap(groupId);
        long total = req.amount();

        List<Portion> payers = buildPayers(req.paidBy(), total, memberById);
        Map<Long, Long> shareAmounts = computeShares(req, total, memberById);
        List<Portion> shares = shareAmounts.entrySet().stream()
                .map(e -> new Portion(e.getKey(), e.getValue()))
                .toList();

        Expense expense = expenses.save(new Expense(group, req.description().trim(), total,
                req.splitType(), payers, shares));
        return toView(expense, memberById);
    }

    @Transactional(readOnly = true)
    public List<ExpenseView> listExpenses(Long groupId) {
        findGroup(groupId);
        Map<Long, Member> memberById = memberMap(groupId);
        return expenses.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(e -> toView(e, memberById))
                .toList();
    }

    public void deleteExpense(Long groupId, Long expenseId) {
        Expense expense = expenses.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khoản chi " + expenseId));
        expenses.delete(expense);
    }

    // ---------------- Số dư & tất toán ----------------

    @Transactional(readOnly = true)
    public List<BalanceView> balances(Long groupId) {
        findGroup(groupId);
        Map<Long, Member> memberById = memberMap(groupId);
        Map<Long, long[]> acc = new LinkedHashMap<>(); // [paid, owed, sent, received]
        memberById.keySet().forEach(id -> acc.put(id, new long[4]));

        for (Expense e : expenses.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            e.getPayers().forEach(p -> acc.get(p.getMemberId())[0] += p.getAmount());
            e.getShares().forEach(s -> acc.get(s.getMemberId())[1] += s.getAmount());
        }
        for (Payment p : payments.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            acc.get(p.getFromMemberId())[2] += p.getAmount();
            acc.get(p.getToMemberId())[3] += p.getAmount();
        }

        List<BalanceView> result = new ArrayList<>();
        acc.forEach((id, a) -> result.add(new BalanceView(id, memberById.get(id).getName(),
                a[0], a[1], a[2], a[3], a[0] - a[1] + a[2] - a[3])));
        return result;
    }

    /** Danh sách chuyển khoản tối thiểu để cả nhóm hết nợ. */
    @Transactional(readOnly = true)
    public List<TransferView> settlements(Long groupId) {
        Map<Long, Long> balanceById = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (BalanceView b : balances(groupId)) {
            balanceById.put(b.memberId(), b.balance());
            names.put(b.memberId(), b.name());
        }
        List<Transfer> transfers = DebtSimplifier.simplify(balanceById);
        return transfers.stream()
                .map(t -> new TransferView(t.from(), names.get(t.from()), t.to(), names.get(t.to()), t.amount()))
                .toList();
    }

    // ---------------- Thanh toán (đánh dấu "đã trả") ----------------

    public PaymentView recordPayment(Long groupId, CreatePaymentRequest req) {
        BillGroup group = findGroup(groupId);
        Map<Long, Member> memberById = memberMap(groupId);
        requireMember(memberById, req.fromMemberId());
        requireMember(memberById, req.toMemberId());
        if (req.fromMemberId().equals(req.toMemberId())) {
            throw new IllegalArgumentException("Người trả và người nhận phải khác nhau");
        }
        Payment p = payments.save(new Payment(group, req.fromMemberId(), req.toMemberId(), req.amount()));
        return toView(p, memberById);
    }

    @Transactional(readOnly = true)
    public List<PaymentView> listPayments(Long groupId) {
        findGroup(groupId);
        Map<Long, Member> memberById = memberMap(groupId);
        return payments.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(p -> toView(p, memberById))
                .toList();
    }

    public void deletePayment(Long groupId, Long paymentId) {
        Payment p = payments.findByIdAndGroupId(paymentId, groupId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thanh toán " + paymentId));
        payments.delete(p);
    }

    // ---------------- Hàm hỗ trợ ----------------

    private List<Portion> buildPayers(List<PayerInput> inputs, long total, Map<Long, Member> memberById) {
        Set<Long> seen = new HashSet<>();
        for (PayerInput p : inputs) {
            requireMember(memberById, p.memberId());
            if (!seen.add(p.memberId())) {
                throw new IllegalArgumentException("Một người chỉ được xuất hiện một lần trong danh sách người trả");
            }
        }
        // Chỉ một người trả và không ghi số tiền => người đó trả toàn bộ.
        if (inputs.size() == 1 && inputs.get(0).amount() == null) {
            return List.of(new Portion(inputs.get(0).memberId(), total));
        }
        long sum = 0;
        List<Portion> result = new ArrayList<>();
        for (PayerInput p : inputs) {
            if (p.amount() == null) {
                throw new IllegalArgumentException("Khi nhiều người cùng trả, cần ghi số tiền của từng người");
            }
            sum += p.amount();
            result.add(new Portion(p.memberId(), p.amount()));
        }
        if (sum != total) {
            throw new IllegalArgumentException(
                    "Tổng tiền những người trả (" + sum + ") phải bằng số tiền hóa đơn (" + total + ")");
        }
        return result;
    }

    private Map<Long, Long> computeShares(CreateExpenseRequest req, long total, Map<Long, Member> memberById) {
        List<SplitInput> splits = req.splitAmong() == null ? List.of() : req.splitAmong();
        Set<Long> seen = new HashSet<>();
        for (SplitInput s : splits) {
            requireMember(memberById, s.memberId());
            if (!seen.add(s.memberId())) {
                throw new IllegalArgumentException("Một người chỉ được xuất hiện một lần trong danh sách chia tiền");
            }
        }
        return switch (req.splitType()) {
            case EQUAL -> SplitCalculator.equal(total,
                    splits.isEmpty() ? memberById.keySet() : splits.stream().map(SplitInput::memberId).toList());
            case EXACT -> SplitCalculator.exact(total, valuesOf(splits, "số tiền"));
            case SHARES -> SplitCalculator.byShares(total, valuesOf(splits, "số phần"));
        };
    }

    private static Map<Long, Long> valuesOf(List<SplitInput> splits, String label) {
        Map<Long, Long> map = new LinkedHashMap<>();
        for (SplitInput s : splits) {
            if (s.value() == null) {
                throw new IllegalArgumentException("Thiếu " + label + " cho thành viên " + s.memberId());
            }
            map.put(s.memberId(), s.value());
        }
        return map;
    }

    private static boolean involves(List<Portion> portions, Long memberId) {
        return portions.stream().anyMatch(p -> p.getMemberId().equals(memberId));
    }

    private static void requireMember(Map<Long, Member> memberById, Long memberId) {
        if (!memberById.containsKey(memberId)) {
            throw new IllegalArgumentException("Thành viên " + memberId + " không thuộc nhóm này");
        }
    }

    private BillGroup findGroup(Long groupId) {
        return groups.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhóm " + groupId));
    }

    private Map<Long, Member> memberMap(Long groupId) {
        return members.findByGroupIdOrderById(groupId).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private GroupView toView(BillGroup g) {
        List<MemberView> memberViews = members.findByGroupIdOrderById(g.getId()).stream()
                .map(m -> new MemberView(m.getId(), m.getName()))
                .toList();
        long totalSpent = expenses.findByGroupIdOrderByCreatedAtDesc(g.getId()).stream()
                .mapToLong(Expense::getAmount)
                .sum();
        return new GroupView(g.getId(), g.getName(), memberViews, totalSpent, g.getCreatedAt());
    }

    private static ExpenseView toView(Expense e, Map<Long, Member> memberById) {
        Function<Portion, PortionView> map = p ->
                new PortionView(p.getMemberId(), memberById.get(p.getMemberId()).getName(), p.getAmount());
        return new ExpenseView(e.getId(), e.getDescription(), e.getAmount(), e.getSplitType(),
                e.getPayers().stream().map(map).toList(),
                e.getShares().stream().map(map).toList(),
                e.getCreatedAt());
    }

    private static PaymentView toView(Payment p, Map<Long, Member> memberById) {
        return new PaymentView(p.getId(),
                p.getFromMemberId(), memberById.get(p.getFromMemberId()).getName(),
                p.getToMemberId(), memberById.get(p.getToMemberId()).getName(),
                p.getAmount(), p.getCreatedAt());
    }
}
