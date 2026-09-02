package Core.enums;

public enum FormationType {
    FOUR_THREE_THREE(4, 3, 3), // 4 defensores, 3 mediocampistas, 3 delanteros
    FOUR_FOUR_TWO(4, 4, 2),    // 4 defensores, 4 mediocampistas, 2 delanteros
    THREE_FIVE_TWO(3, 5, 2),   // 3 defensores, 5 mediocampistas, 2 delanteros
    FOUR_FIVE_ONE(4, 5, 1);    // 4 defensores, 5 mediocampistas, 1 delantero

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