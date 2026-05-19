package senpai.asm;

import org.objectweb.asm.Type;

public final class DescriptorTools {

    public static String mapDescriptor(String descriptor, java.util.function.Function<String, String> classMapper) {
        Type type = Type.getType(descriptor);
        return remap(type, classMapper).getDescriptor();
    }

    public static String mapMethodDescriptor(String descriptor, java.util.function.Function<String, String> classMapper) {
        Type method = Type.getMethodType(descriptor);
        Type[] args = method.getArgumentTypes();
        Type[] mapped = new Type[args.length];
        for (int i = 0; i < args.length; i++) {
            mapped[i] = remap(args[i], classMapper);
        }
        Type ret = remap(method.getReturnType(), classMapper);
        return Type.getMethodDescriptor(ret, mapped);
    }

    private static Type remap(Type type, java.util.function.Function<String, String> classMapper) {
        return switch (type.getSort()) {
            case Type.OBJECT -> Type.getObjectType(classMapper.apply(type.getInternalName()));
            case Type.ARRAY -> {
                Type element = remap(type.getElementType(), classMapper);
                StringBuilder prefix = new StringBuilder();
                for (int i = 0; i < type.getDimensions(); i++) {
                    prefix.append('[');
                }
                yield Type.getType(prefix.toString() + element.getDescriptor());
            }
            default -> type;
        };
    }
}
