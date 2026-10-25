import com.nuttyvr.common.ModRules;

public final class ModRulesTest {
    private static int passed;
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { passed++; return; }
        throw new AssertionError("Invalid value was accepted");
    }
    private static void check(boolean value) { if (!value) throw new AssertionError("Expected true"); passed++; }
    public static void main(String[] args) {
        check(ModRules.id("mint-rush").equals("mint-rush"));
        check(ModRules.target("com.beatgames.beatsaber").equals("com.beatgames.beatsaber"));
        check(ModRules.target("com.nuttyvr.demo").equals("com.nuttyvr.demo"));
        check(ModRules.version("0.1.0").equals("0.1.0"));
        check(ModRules.version("1.37.0_42").equals("1.37.0_42"));
        check(ModRules.color("#A9EF64").equals("#A9EF64"));
        check(ModRules.speed(0.5) == 0.5); check(ModRules.speed(2.0) == 2.0);
        check(ModRules.compatible("com.nuttyvr.demo", "0.1.0", "com.nuttyvr.demo", "0.1.0"));
        check(!ModRules.compatible("com.nuttyvr.demo", "0.1.0", "com.beatgames.beatsaber", "0.1.0"));
        check(!ModRules.compatible("com.nuttyvr.demo", "0.1.0", "com.nuttyvr.demo", "0.1.1"));
        rejects(() -> ModRules.id("../escape")); rejects(() -> ModRules.id("x/y")); rejects(() -> ModRules.id("a")); rejects(() -> ModRules.id(null));
        rejects(() -> ModRules.target("com.demo; id")); rejects(() -> ModRules.target("com..demo")); rejects(() -> ModRules.target("com.demo/Activity"));
        rejects(() -> ModRules.version("*")); rejects(() -> ModRules.version("1.0\n"));
        rejects(() -> ModRules.color("#fff")); rejects(() -> ModRules.color("#GGGGGG"));
        rejects(() -> ModRules.speed(0.49)); rejects(() -> ModRules.speed(2.01));
        rejects(() -> ModRules.speed(Double.NaN)); rejects(() -> ModRules.speed(Double.POSITIVE_INFINITY));
        rejects(() -> ModRules.text("  ", 64, "name")); rejects(() -> ModRules.text("Name\u0000", 64, "name"));
        rejects(() -> ModRules.text("Too long", 3, "name"));
        System.out.println(passed + " profile-rule assertions passed.");
    }
}
