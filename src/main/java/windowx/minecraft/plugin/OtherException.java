package windowx.minecraft.plugin;

public class OtherException extends Exception {
    public OtherException(String e) {
        super.addSuppressed(new Throwable(e));
    }
    // Other things...
}
