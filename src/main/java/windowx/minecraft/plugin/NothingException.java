package windowx.minecraft.plugin;

public class NothingException extends Exception {
    public NothingException() {
        super.addSuppressed(new Throwable());
    }
    // Nothing...
}
