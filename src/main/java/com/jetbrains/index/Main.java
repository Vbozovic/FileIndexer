package com.jetbrains.index;

import com.jetbrains.index.index.IndexSearchService;
import com.jetbrains.index.token.factory.CachingTokenFactory;
import com.jetbrains.index.token.tokenizer.WhiteSpaceTokenizer;
import com.jetbrains.index.watcher.FileSystemWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        List<String> paths = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("-p")) {
                paths.add(arg.substring("-p".length()));
            }
        }

        try (FileSystemWatcher watcher = new FileSystemWatcher(paths)) {
            IndexSearchService svc = new IndexSearchService(new WhiteSpaceTokenizer(CachingTokenFactory.getInstance()));
            watcher.registerListener(svc);
            watcher.start();

            Scanner scanner = new Scanner(System.in);
            CommandMode currentMode = CommandMode.MENU;
            System.out.println("In menu:");
            do {
                String line;

                switch (currentMode) {
                    case MENU -> {
                        System.out.println("Available commands: search,menu(to go back to the menu)");
                        currentMode = nextMode(scanner.next());
                    }
                    case QUERY -> {
                        line = scanner.next();
                        if (nextMode(line) != CommandMode.MENU) {
                            var found = svc.findWord(line);
                            System.out.println("Result: ");
                            found.forEach(System.out::println);
                        } else {
                            currentMode = CommandMode.MENU;
                        }
                    }
                }

            } while (scanner.hasNextLine());
        }
    }


    private static CommandMode nextMode(String line) {
        return switch (line) {
            case "search" -> CommandMode.QUERY;
            case "menu" -> CommandMode.MENU;
            case null, default -> CommandMode.NONE;
        };
    }

    private enum CommandMode {
        MENU, QUERY, NONE
    }


}
