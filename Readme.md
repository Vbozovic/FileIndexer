# The modest "File Indexer"
In order to showcase the actual workings of the indexer run the application with commandline arguments. Only one commandline argument is supported, `-p<path>` which needs to resolve either to a directory or a file. If the directory is provided then all subdirectories and files are recursively inspected and indexed. Once you run the application you can use the terminal to search for phrases. Typing in `search` will put you in search mode. All text you type in will be considered a single word. For example:
```text
arguments: -psrc/main/java -psrc/test/resources

... log statements

In menu:
Available commands: search,menu,quit(to go back to the menu)

-> search
-> secret

Result: 
/Users/vbozovic/usavrsavanje/FileIndexer/src/test/resources/TestFile2.txt

```

Manually deleting or editing a file will trigger either the removal of that file from the index or it's update.