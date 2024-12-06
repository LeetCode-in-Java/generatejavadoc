package generatejavadoc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    private static final Pattern PATTERN = Pattern.compile("\\r?\\n");

    private static void fillFilesRecursively(Path directory, final List<File> resultFiles)
            throws IOException {
        Files.walkFileTree(
                directory,
                new java.nio.file.SimpleFileVisitor<Path>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(
                            Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                        final String filePath = file.toString().toLowerCase();
                        if (filePath.endsWith("readme.md")) {
                            resultFiles.add(file.toFile());
                        }
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });
    }

    public static void main(String[] args) throws IOException {
        List<File> resultFiles = new ArrayList<>();
        fillFilesRecursively(Paths.get("."), resultFiles);
        int ind = 0;
        for (File file : resultFiles) {
            if (ind % 20 == 0) {
                System.out.println(file.getAbsolutePath());
            }
            ind++;
            final byte[] readmeMd = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
            String readmeMdText = new String(readmeMd, UTF_8);
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.{java,kt}");
            try (DirectoryStream<Path> dirStream =
                    Files.newDirectoryStream(
                            Paths.get(file.getAbsolutePath().replace("readme.md", "")))) {
                for (Path entry : dirStream) {
                    if (matcher.matches(entry.getFileName())) {
                        File javaFile = entry.toFile();
                        Path path = Paths.get(javaFile.getAbsolutePath());
                        final byte[] solutionJava = Files.readAllBytes(path);
                        String solutionJavaText = new String(solutionJava, UTF_8);
                        int[] index = {0};
                        String[] fromStr = {
                            "(\"{{",
                            "= \"{{",
                            "\n- [",
                            "    board = [[",
                            "    grid = [[",
                            " = [[",
                            "**,",
                            "**]",
                            "(**",
                            "**)",
                            "[[]",
                            "<code>",
                            "</code>",
                            "<sub>",
                            "</sub>",
                            "<sup>",
                            "</sup>",
                            "<ins>",
                            "</ins>",
                            "<",
                            ">",
                            "&"
                        };
                        String[] fromStr2 = {
                            "[code]", "[/code]", "[sub]", "[/sub]", "[sup]", "[/sup]", "[ins]",
                            "[/ins]",
                        };
                        String[] toStr = {
                            "(\"{ {",
                            "= \"{ {",
                            "\n- \\[",
                            "    board = [ [",
                            "    grid = [ [",
                            " = \\[\\[",
                            "** ,",
                            "** ]",
                            "( **",
                            "** )",
                            "[ []",
                            "[code]",
                            "[/code]",
                            "[sub]",
                            "[/sub]",
                            "[sup]",
                            "[/sup]",
                            "[ins]",
                            "[/ins]",
                            "&lt;",
                            "&gt;",
                            "&amp;"
                        };
                        String[] toStr2 = {
                            "<code>", "</code>", "<sub>", "</sub>", "<sup>", "</sup>", "<ins>",
                            "</ins>",
                        };
                        String javaLinks =
                                " * [![](https://img.shields.io/github/stars/javadev/LeetCode-in-Java?style=flat-square)](https://github.com/javadev/LeetCode-in-Java)\n"
                                        + " * [![](https://img.shields.io/github/forks/javadev/LeetCode-in-Java?style=flat-square)](https://github.com/javadev/LeetCode-in-Java/fork)\n"
                                        + " *\n";
                        String kotlinLinks =
                                " * [![](https://img.shields.io/github/stars/javadev/LeetCode-in-Kotlin?style=flat-square)](https://github.com/javadev/LeetCode-in-Kotlin)\n"
                                        + " * [![](https://img.shields.io/github/forks/javadev/LeetCode-in-Kotlin?style=flat-square)](https://github.com/javadev/LeetCode-in-Kotlin/fork)\n"
                                        + " *\n";
                        String readmeMdJavadoc =
                                "/**\n"
                                        + (javaFile.getAbsolutePath().endsWith(".java")
                                                ? javaLinks
                                                : javaFile.getAbsolutePath().endsWith(".kt")
                                                        ? kotlinLinks
                                                        : "")
                                        + StringUtils.replaceEach(
                                                        StringUtils.replaceEach(
                                                                PATTERN.splitAsStream(readmeMdText)
                                                                        .map(
                                                                                line ->
                                                                                        getString(
                                                                                                line,
                                                                                                index))
                                                                        .collect(
                                                                                Collectors.joining(
                                                                                        "\n")),
                                                                fromStr,
                                                                toStr),
                                                        fromStr2,
                                                        toStr2)
                                                .replace("`**", "` **")
                                                .replace(",**", ", **")
                                                .replace("<ins>**", "<ins> **")
                                                .replace("**</ins>", "** </ins>")
                                                .replace("/*", "{@literal /}*")
                                                .replace("*/", "*{@literal /}")
                                        + "\n**/";
                        String publicClass =
                                solutionJavaText.contains("@SuppressWarnings")
                                        ? "@SuppressWarnings"
                                        : solutionJavaText.contains("public class ")
                                                ? "public class "
                                                : "class ";
                        Files.write(path, getBytes(solutionJavaText, readmeMdJavadoc, publicClass));
                    }
                }
            }
        }
    }

    private static String getString(String line, int[] index) {
        String firstLine = line.replace("\\.", " -") + "\\.";
        String str = index[0]++ == 0 ? firstLine : line;
        return line.isEmpty() ? " *" : " * " + str;
    }

    private static byte[] getBytes(
            String solutionJavaText, String readmeMdJavadoc, String publicClass) {
        return solutionJavaText
                .replace("\n" + publicClass, "\n" + readmeMdJavadoc + "\n" + publicClass)
                .getBytes(UTF_8);
    }
}
