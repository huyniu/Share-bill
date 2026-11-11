package com.sharebill.service;

import com.sharebill.core.SplitType;
import com.sharebill.web.ApiDtos.BalanceView;
import com.sharebill.web.ApiDtos.CreateExpenseRequest;
import com.sharebill.web.ApiDtos.CreateGroupRequest;
import com.sharebill.web.ApiDtos.CreatePaymentRequest;
import com.sharebill.web.ApiDtos.GroupView;
import com.sharebill.web.ApiDtos.PayerInput;
import com.sharebill.web.ApiDtos.SplitInput;
import com.sharebill.web.ApiDtos.TransferView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GroupServiceTest {

    @Autowired
    GroupService service;

    @Test
    void multiplePayersAndSettlement() {
        GroupView g = service.createGroup(new CreateGroupRequest("Ăn tối", List.of("An", "Bình", "Chi", "Dũng")));
        Long an = g.members().get(0).id();
        Long binh = g.members().get(1).id();
        Long chi = g.members().get(2).id();
        Long dung = g.members().get(3).id();

        // An trả 800k, Bình trả 400k, chia đều cho cả nhóm
        service.addExpense(g.id(), new CreateExpenseRequest("Lẩu", 1_200_000L,
                List.of(new PayerInput(an, 800_000L), new PayerInput(binh, 400_000L)),
                SplitType.EQUAL, null));

        List<BalanceView> balances = service.balances(g.id());
        assertEquals(List.of(500_000L, 100_000L, -300_000L, -300_000L),
                balances.stream().map(BalanceView::balance).toList());

        List<TransferView> transfers = service.settlements(g.id());
        assertEquals(3, transfers.size());
        assertEquals("Chi", transfers.get(0).fromName());
        assertEquals("An", transfers.get(0).toName());
        assertEquals(300_000L, transfers.get(0).amount());

        // Chi chuyển cho An => Chi hết nợ
        service.recordPayment(g.id(), new CreatePaymentRequest(chi, an, 300_000L));
        assertEquals(0L, service.balances(g.id()).get(2).balance());
        assertEquals(2, service.settlements(g.id()).size());

        // Dũng trả nốt
        service.recordPayment(g.id(), new CreatePaymentRequest(dung, an, 200_000L));
        service.recordPayment(g.id(), new CreatePaymentRequest(dung, binh, 100_000L));
        assertTrue(service.settlements(g.id()).isEmpty());
    }

    @Test
    void sharesAndSubsetSplit() {
        GroupView g = service.createGroup(new CreateGroupRequest("Nhậu", List.of("An", "Bình", "Chi")));
        Long an = g.members().get(0).id();
        Long binh = g.members().get(1).id();
        Long chi = g.members().get(2).id();

        // Bia 300k: An trả, chỉ An (2 phần) và Bình (1 phần) uống, Chi không uống
        service.addExpense(g.id(), new CreateExpenseRequest("Bia", 300_000L,
                List.of(new PayerInput(an, null)),
                SplitType.SHARES, List.of(new SplitInput(an, 2L), new SplitInput(binh, 1L))));

        List<BalanceView> b = service.balances(g.id());
        assertEquals(100_000L, b.get(0).balance());
        assertEquals(-100_000L, b.get(1).balance());
        assertEquals(0L, b.get(2).balance());
        assertEquals(0L, b.get(2).owed());
        assertEquals(chi, b.get(2).memberId());
    }

    @Test
    void rejectsPayersNotMatchingTotal() {
        GroupView g = service.createGroup(new CreateGroupRequest("Test", List.of("An", "Bình")));
        Long an = g.members().get(0).id();
        Long binh = g.members().get(1).id();

        assertThrows(IllegalArgumentException.class, () -> service.addExpense(g.id(),
                new CreateExpenseRequest("Sai", 100_000L,
                        List.of(new PayerInput(an, 50_000L), new PayerInput(binh, 40_000L)),
                        SplitType.EQUAL, null)));
    }

    @Test
    void rejectsMemberFromAnotherGroup() {
        GroupView g1 = service.createGroup(new CreateGroupRequest("G1", List.of("An")));
        GroupView g2 = service.createGroup(new CreateGroupRequest("G2", List.of("Bình")));

        assertThrows(IllegalArgumentException.class, () -> service.addExpense(g1.id(),
                new CreateExpenseRequest("Sai", 100_000L,
                        List.of(new PayerInput(g2.members().get(0).id(), null)),
                        SplitType.EQUAL, null)));
    }
}
