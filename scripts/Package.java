import dev.mccue.tools.jar.Jar;
import dev.mccue.tools.javac.Javac;
import dev.mccue.tools.jlink.JLink;
import dev.mccue.tools.jpackage.JPackage;

void main() throws Exception {

    List<Path> sourceFiles;
    try (var paths = Files.find(
            Path.of("src"),
            Integer.MAX_VALUE,
            (_, attributes) -> attributes.isRegularFile()
    )) {
        sourceFiles = paths.toList();
    }

    Javac.run(args -> {
        args._d(Path.of("build", "classes"))
                ._g()
                .__release(17)
                .arguments(sourceFiles);
    });

    Jar.run(args -> {
        args.__create().__file(Path.of("build", "jar", "game.jar"))
                .__main_class("Main")
                ._C(Path.of("build", "classes"), ".");
    });

    Files.copy(Path.of("build", "jar", "game.jar"), Path.of("site", "game.jar"), StandardCopyOption.REPLACE_EXISTING);

}