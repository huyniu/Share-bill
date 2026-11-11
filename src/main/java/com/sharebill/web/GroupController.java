package com.sharebill.web;

import com.sharebill.service.GroupService;
import com.sharebill.web.ApiDtos.AddMemberRequest;
import com.sharebill.web.ApiDtos.BalanceView;
import com.sharebill.web.ApiDtos.CreateExpenseRequest;
import com.sharebill.web.ApiDtos.CreateGroupRequest;
import com.sharebill.web.ApiDtos.CreatePaymentRequest;
import com.sharebill.web.ApiDtos.ExpenseView;
import com.sharebill.web.ApiDtos.GroupView;
import com.sharebill.web.ApiDtos.MemberView;
import com.sharebill.web.ApiDtos.PaymentView;
import com.sharebill.web.ApiDtos.TransferView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService service;

    public GroupController(GroupService service) {
        this.service = service;
    }

    // ----- Nhóm -----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupView createGroup(@Valid @RequestBody CreateGroupRequest req) {
        return service.createGroup(req);
    }

    @GetMapping
    public List<GroupView> listGroups() {
        return service.listGroups();
    }

    @GetMapping("/{groupId}")
    public GroupView getGroup(@PathVariable Long groupId) {
        return service.getGroup(groupId);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long groupId) {
        service.deleteGroup(groupId);
    }

    // ----- Thành viên -----

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberView addMember(@PathVariable Long groupId, @Valid @RequestBody AddMemberRequest req) {
        return service.addMember(groupId, req.name());
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        service.removeMember(groupId, memberId);
    }

    // ----- Khoản chi -----

    @PostMapping("/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseView addExpense(@PathVariable Long groupId, @Valid @RequestBody CreateExpenseRequest req) {
        return service.addExpense(groupId, req);
    }

    @GetMapping("/{groupId}/expenses")
    public List<ExpenseView> listExpenses(@PathVariable Long groupId) {
        return service.listExpenses(groupId);
    }

    @DeleteMapping("/{groupId}/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long groupId, @PathVariable Long expenseId) {
        service.deleteExpense(groupId, expenseId);
    }

    // ----- Số dư & tất toán -----

    @GetMapping("/{groupId}/balances")
    public List<BalanceView> balances(@PathVariable Long groupId) {
        return service.balances(groupId);
    }

    /** Gợi ý ai cần chuyển cho ai bao nhiêu (đã tối giản số giao dịch). */
    @GetMapping("/{groupId}/settlements")
    public List<TransferView> settlements(@PathVariable Long groupId) {
        return service.settlements(groupId);
    }

    // ----- Thanh toán -----

    @PostMapping("/{groupId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentView recordPayment(@PathVariable Long groupId, @Valid @RequestBody CreatePaymentRequest req) {
        return service.recordPayment(groupId, req);
    }

    @GetMapping("/{groupId}/payments")
    public List<PaymentView> listPayments(@PathVariable Long groupId) {
        return service.listPayments(groupId);
    }

    @DeleteMapping("/{groupId}/payments/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable Long groupId, @PathVariable Long paymentId) {
        service.deletePayment(groupId, paymentId);
    }
}
