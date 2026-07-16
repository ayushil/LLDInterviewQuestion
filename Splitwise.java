/*
Groups
Add expense - equal, percentage, by amount - done
View Balances -
Settlement - normal settlement | simplify balance

*
* */

import java.util.*;

public class Splitwise {
    public static void main(String[] args) {
        User a = new User(1);
        User b = new User(2);
        User c = new User(3);
        User d = new User(4);

        List<Participant> participants = new ArrayList<>();
        Participant participantA = new Participant(1, a);
        Participant participantB = new Participant(2, b);
        Participant participantC = new Participant(3, c);
        Participant participantD = new Participant(4, d);

        participants.add(participantA);
        participants.add(participantB);
        participants.add(participantC);
        participants.add(participantD);

        SettlementStrategy settlementStrategy = new SimplifySettlementStrategy();
//        SettlementStrategy simpleSettlementStrategy = new SimpleSettlementStrategy();

        Group group = new Group(participants, settlementStrategy);

        Expense expense = new Expense(900, participantA, participants, new EqualSplitStrategy());

        group.viewBalances();
        group.addExpense(expense);
        group.viewBalances();

        Expense expense2 = new Expense(350, participantB, participants, new EqualSplitStrategy());
        group.addExpense(expense2);
        group.viewBalances();

        Expense expense3 = new Expense(150, participantC, participants, new EqualSplitStrategy());
        group.addExpense(expense3);
        group.viewBalances();

        Expense expense4 = new Expense(200, participantD, participants, new EqualSplitStrategy());
        group.addExpense(expense4);
        group.viewBalances();

        group.settle();
    }
}

class SplitWiseApp {
    List<Group> groups;

}

class Group {
    HashMap<Integer, Participant> participantHashMap;
    List<Expense> expenses;
    SettlementStrategy settlementStrategy;

    public Group (List<Participant> participants, SettlementStrategy settlementStrategy) {
        this.participantHashMap = new HashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            participantHashMap.put(p.participantId, p);
        }
        this.settlementStrategy = settlementStrategy;
        this.expenses = new ArrayList<>();
    }

    public void addExpense(Expense e) {
        e.expenseSplitStrategy.split(this, e);
    }

    public void viewBalances() {
        for (Integer participantId : participantHashMap.keySet()) {
            Participant p = participantHashMap.get(participantId);
            System.out.println(participantId + " " + p.amount);
        }
    }

    public void settle() {
        this.settlementStrategy.settle(this);
    }

}

class Participant {
    int participantId;
    User user;
    int amount;

    public Participant(int participantId, User user) {
        this.participantId = participantId;
        this.user = user;
        this.amount = 0;
    }
}

class User {
    int userId;

    public User(int userId) {
        this.userId = userId;
    }
}

class Expense {
    int amount;
    Participant paidBy;
    List<Participant> participantsInvolved;
    List<Integer> percentageSplit;
    ExpenseSplitStrategy expenseSplitStrategy;

    public Expense(int amount, Participant paidBy, List<Participant> participants, ExpenseSplitStrategy expenseSplitStrategy) {
        this.amount = amount;
        this.paidBy = paidBy;
        this.participantsInvolved = participants;
        this.expenseSplitStrategy = expenseSplitStrategy;
    }

    public Expense(int amount, Participant paidBy, List<Participant> participants, ExpenseSplitStrategy expenseSplitStrategy, List<Integer> percentageSplit) {
        this.amount = amount;
        this.paidBy = paidBy;
        this.percentageSplit = percentageSplit;
        this.participantsInvolved = participants;
        this.expenseSplitStrategy = expenseSplitStrategy;
    }
}

interface ExpenseSplitStrategy {
    public void split(Group g, Expense e);
}

class EqualSplitStrategy implements ExpenseSplitStrategy {
    public void split(Group g, Expense e) {
        int amountSplit = e.amount / e.participantsInvolved.size();
        for (Participant p : e.participantsInvolved) {
            Participant participant = g.participantHashMap.get(p.participantId);
            if (p.participantId == e.paidBy.participantId) {
                participant.amount += e.amount - amountSplit;
            } else {
                participant.amount -= amountSplit;
            }
        }
    }
}


class PercentageSplitStrategy implements ExpenseSplitStrategy {
    public void split(Group g, Expense e) {
        int i = 0;
        for (Participant p : e.participantsInvolved) {
            int amountSplit = e.amount * e.percentageSplit.get(i);
            i++;
            Participant participant = g.participantHashMap.get(p.participantId);
            if (p.participantId == e.paidBy.participantId) {
                participant.amount += e.amount - amountSplit;
            } else {
                participant.amount -= amountSplit;
            }
        }
    }
}

interface SettlementStrategy {
    public void settle(Group group);
}

class SimplifySettlementStrategy implements SettlementStrategy {
    public void settle(Group group) {
        PriorityQueue<Participant> creditors = new PriorityQueue<>(new Comparator<Participant>() {
            public int compare(Participant o1, Participant o2) {
                return o2.amount - o1.amount;
            }
        });
        PriorityQueue<Participant> debitors = new PriorityQueue<>(new Comparator<Participant>() {
            public int compare(Participant o1, Participant o2) {
                return o1.amount - o2.amount;
            }
        });

        for (int participantId : group.participantHashMap.keySet()) {
            Participant p = group.participantHashMap.get(participantId);
            if (p.amount < 0) {
                debitors.add(p);
            } else if (p.amount > 0) {
                creditors.add(p);
            }
        }

        while (creditors.size() > 0 && debitors.size() > 0) {
            Participant creditor = creditors.poll();
            Participant debitor = debitors.poll();

            System.out.println("Creditor " + creditor.participantId + " " + "Debitor " + debitor.participantId + "|" + creditor.amount + " <--- " + debitor.amount);
            creditor.amount += debitor.amount;
            if (creditor.amount != 0) {
                creditors.add(creditor);
            }
        }
    }
}



class SimpleSettlementStrategy implements SettlementStrategy {
    public void settle(Group group) {
        List<Participant> creditors = new ArrayList<>();
        List<Participant> debitors = new ArrayList<>();

        for (int participantId: group.participantHashMap.keySet()) {
            Participant p = group.participantHashMap.get(participantId);
            if (p.amount < 0) {
                debitors.add(p);
            } else if (p.amount > 0) {
                creditors.add(p);
            }
        }

        int i = 0, j = 0;
        while (i < creditors.size() && j < debitors.size()) {
            Participant c = creditors.get(i);
            Participant d = debitors.get(j);
            if (c.amount > (-1*d.amount)) {
                j++;
                System.out.println("Creditor " + c.participantId + " " + "Debitor " + d.participantId + "|" + c.amount + " <--- " + d.amount);
                c.amount += d.amount;
            } else {
                i++;
                System.out.println("Creditor " + c.participantId + " " + "Debitor " + d.participantId + "|" + c.amount + " <--- " + d.amount);
                d.amount += c.amount;
            }
        }
    }
}