package pl.betoncraft.betonquest.notify;

import org.bukkit.entity.Player;
import pl.betoncraft.betonquest.exceptions.InstructionParseException;

import java.util.Map;

public class SubTitleNotifyIO extends NotifyIO {

    private final int fadeIn;
    private final int stay;
    private final int fadeOut;


    public SubTitleNotifyIO(final Map<String, String> data) throws InstructionParseException {
        super(data);

        fadeIn = getIntegerData("fadein", 10);
        stay = getIntegerData("stay", 70);
        fadeOut = getIntegerData("fadeout", 20);
    }

    @Override
    protected void sendNotify(final String message, final Player player) {
        player.sendTitle("", message, fadeIn, stay, fadeOut);
    }
}
