import java.io.File;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.client.main.Main;

/**
 * @author Liticane
 * Allows using existing .minecraft directory during testing.
 */
public class Start {

    // I am tired of copying around assets every single time.
    public static void main(String[] args) {
        OptionParser optionparser = new OptionParser();
        optionparser.allowsUnrecognizedOptions();

        OptionSpec<String> uid = optionparser.accepts("uid").withRequiredArg();
        OptionSet optionset = optionparser.parse(args);

        final String appdataDirectory = System.getenv("APPDATA");

        File runDir = new File(appdataDirectory != null ? appdataDirectory : System.getProperty("user.home", "."), ".minecraft/");

        File gameDir = new File(runDir, ".");
        File assetsDir = new File(runDir, "assets/");

        Main.main(new String[] {
                "--version", "1.8.9",
                "--accessToken", "0",
                "--assetIndex", "1.8",
                "--userProperties", "{}",
                "--gameDir", gameDir.getAbsolutePath(),
                "--assetsDir", assetsDir.getAbsolutePath(),
                "--uid=" + optionset.valueOf(uid)
        });
    }

}