package de.firemage.autograder.core.integrated;

import de.firemage.autograder.core.LinterStatus;
import de.firemage.autograder.core.file.UploadedFile;
import spoon.Launcher;
import spoon.compiler.ModelBuildingException;
import spoon.processing.Processor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class StaticAnalysis implements AutoCloseable {
    private final URLClassLoader classLoader;
    private final Factory factory;
    private final CtModel model;

    public StaticAnalysis(UploadedFile file, Path jar, Consumer<LinterStatus> statusConsumer) throws ModelBuildException, IOException {
        statusConsumer.accept(LinterStatus.BUILDING_CODE_MODEL);

        // Use a custom class loader because spoon won't close its standard URLClassLoader and will leak the handle to the jar file
        this.classLoader =
            new URLClassLoader(new URL[] {jar.toUri().toURL()}, Thread.currentThread().getContextClassLoader());

        Launcher launcher = new Launcher();
        launcher.addInputResource(file.getSpoonFile());
        launcher.getEnvironment().setShouldCompile(false);
        launcher.getEnvironment().setSourceClasspath(new String[] {jar.toAbsolutePath().toString()});
        launcher.getEnvironment().setNoClasspath(false);
        launcher.getEnvironment().setCommentEnabled(true);
        launcher.getEnvironment().setComplianceLevel(file.getVersion().getVersionNumber());
        launcher.getEnvironment().setInputClassLoader(classLoader);
        launcher.getEnvironment().setEncoding(file.getCharset());

        try {
            this.model = launcher.buildModel();
        } catch (ModelBuildingException e) {
            throw new ModelBuildException("Failed to parse the code", e);
        }
        this.factory = launcher.getFactory();
    }

    public Factory getFactory() {
        return factory;
    }

    public CtModel getModel() {
        return model;
    }

    public <E extends CtElement> void processWith(Processor<E> processor) {
        this.model.processWith(processor);
    }

    public CtClass<?> findClassByName(String name) {
        CtClass<?> clazz = this.model.filterChildren(
            child -> child instanceof CtClass<?> c && c.getQualifiedName().equals(name)).first();
        if (clazz == null) {
            throw new IllegalArgumentException("No class with name '" + name + "' found");
        }
        return clazz;
    }

    public CtMethod<?> findMethodBySignature(CtClass<?> clazz, String signature) {
        CtMethod<?> result = this.model.filterChildren(
            child -> child instanceof CtMethod<?> method && method.getSignature().equals(signature)).first();
        if (result == null) {
            throw new IllegalArgumentException("No method in class " + clazz.getQualifiedName() + " with signature '" + signature + "' found");
        }
        return result;
    }

    public CtMethod<Void> findMain() {
        return this.model.filterChildren(child -> child instanceof CtMethod<?> method && isMain(method))
            .map(c -> (CtMethod<Void>) c)
            .first();
    }

    public List<String> getAllPackageNames() {
        return this.model.filterChildren(c -> c instanceof CtPackage).map(p -> ((CtPackage) p).getQualifiedName()).list();
    }

    @Override
    public void close() throws IOException {
        this.classLoader.close();
    }

    private boolean isMain(CtMethod<?> method) {
        return method.getSimpleName().equals("main")
            && method.getType().getQualifiedName().equals("void")
            && method.isStatic()
            && method.isPublic()
            && method.getParameters().size() == 1
            && method.getParameters().get(0).getType().getQualifiedName().equals("java.lang.String[]");
    }
}
