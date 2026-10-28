package qa.eca;

/** Synthetic ECA review fixture. Not production code; do not merge. */
public final class DeliveryCost {
    public int calculate(int quantity, int unitPrice) {
        return Math.multiplyExact(quantity, unitPrice);
    }
}
