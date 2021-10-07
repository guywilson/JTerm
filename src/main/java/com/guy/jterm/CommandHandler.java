package com.guy.jterm;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;

public class CommandHandler
{
    private boolean inCommandMode = false;

    public boolean isCommandMode() {
        return inCommandMode;
    }

    public void setCommandMode(boolean on) {
        this.inCommandMode = on;
    }

    public CommandHandler(LineReader reader) {
        KeyMap<Binding> map = reader.getKeyMaps().get(LineReader.MAIN);
        map.bind(new Reference("command-widget"), KeyMap.ctrl('A'));
                
        reader.getWidgets().put("command-widget", () -> {
            System.out.println();

            this.inCommandMode = true;
            
            return true;
        });
    }
}
