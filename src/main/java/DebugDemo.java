public class DebugDemo {
    static int calculateTotal(int price, int count) {
        int total = price * count;
        return total;
    }
    public static void main(String[] args) {
        int price = 120;
        int count = 3;
        int result = calculateTotal(price, count);
        System.out.println("Total: " + result);
    }
}
