package Core.enums;

public enum FormationType {
    FOUR_THREE_THREE(4, 3, 3), // 4 defenders, 3 midfielders, 3 forwards
    FOUR_FOUR_TWO(4, 4, 2),    // 4 defenders, 4 midfielders, 2 forwards
    THREE_FIVE_TWO(3, 5, 2),   // 3 defenders, 5 midfielders, 2 forwards
    FOUR_FIVE_ONE(4, 5, 1);    // 4 defenders, 5 midfielders, 1 forward

    private final int defenders;
    private final int midfielders;
    private final int forwards;

    FormationType(int defenders, int midfielders, int forwards) {
        this.defenders = defenders;
        this.midfielders = midfielders;
        this.forwards = forwards;
    }

    public int getDefenders() { return defenders; }
    public int getMidfielders() { return midfielders; }
    public int getForwards() { return forwards; }
}