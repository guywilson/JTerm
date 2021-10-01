package com.guy.jterm;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.LineReader;
import org.jline.reader.Reference;

public class CommandHandler
{
    public CommandHandler(LineReader reader) {
        KeyMap<Binding> map = reader.getKeyMaps().get(LineReader.MAIN);
        map.bind(new Reference("command-widget"), KeyMap.ctrl('A'));
                
        reader.getWidgets().put("command-widget", () -> {
            System.out.println("Got command!");            
            return true;
        });
    }


}
