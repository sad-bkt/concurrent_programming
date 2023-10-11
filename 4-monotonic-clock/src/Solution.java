import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Семина Анастасия
 */
public class Solution implements MonotonicClock {
    private final RegularInt c1 = new RegularInt(0);
    private final RegularInt c2 = new RegularInt(0);
    private final RegularInt c3 = new RegularInt(0);

    private final RegularInt c1copy = new RegularInt(0);
    private final RegularInt c2copy = new RegularInt(0);
    @Override
    public void write(@NotNull Time time) {
        // write left-to-right
        c1.setValue(time.getD1());
        c2.setValue(time.getD2());
        c3.setValue(time.getD3());

        // write right-to-left + optimization
        c2copy.setValue(time.getD2());
        c1copy.setValue(time.getD1());
    }

    @NotNull
    @Override
    public Time read() {
        // read left-to-right
        int u1copy = c1copy.getValue();
        int u2copy = c2copy.getValue();

        // read right-to-left + optimization
        int u3 = c3.getValue();
        int u2 = c2.getValue();
        int u1 = c1.getValue();
        if (u1 == u1copy) {
            if (u2 == u2copy) {
                return new Time(u1, u2, u3);
            }
            return new Time(u1, u2, 0);
        }
        return new Time(u1, 0, 0);
    }
}
