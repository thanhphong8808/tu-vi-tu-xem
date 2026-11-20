package vn.tuvi.indyfix;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.tree.*;

/**
 * Chép một jar, bỏ các gói không dùng được trên Android và thay thân các hàm có invokedynamic trong
 * kotlin/comparisons/ComparisonsKt__ComparisonsKt bằng lời gọi sang vn.tuvi.indyfix.Comparators.
 * Dừng với lỗi nếu còn invokedynamic ở bất kỳ đâu khác.
 * Dùng: java -cp kotlin-compiler.jar:. vn.tuvi.indyfix.StripIndy in.jar out.jar
 */
public final class StripIndy {
    static final String TARGET = "kotlin/comparisons/ComparisonsKt__ComparisonsKt";
    static final String HELPER = "vn/tuvi/indyfix/Comparators";
    static final Set<String> FIXABLE = new HashSet<>(Arrays.asList("compareBy", "then", "thenDescending", "nullsFirst", "nullsLast"));
    static final String[] DROP_PREFIXES = {"kotlin/uuid/", "kotlin/streams/"};

    public static void main(String[] args) throws Exception {
        int fixed = 0, dropped = 0;
        List<String> leftovers = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(args[0]));
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(args[1]))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                String name = e.getName();
                if (!name.endsWith(".class") || name.startsWith("META-INF/") || name.endsWith("module-info.class")) continue;
                boolean drop = false;
                for (String p : DROP_PREFIXES) if (name.startsWith(p)) drop = true;
                if (drop) { dropped++; continue; }
                byte[] bytes = in.readAllBytes();
                ClassNode cn = new ClassNode();
                new ClassReader(bytes).accept(cn, 0);
                boolean changed = false;
                for (MethodNode m : cn.methods) {
                    if (!hasIndy(m)) continue;
                    boolean isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
                    if (cn.name.equals(TARGET) && FIXABLE.contains(m.name) && isStatic) {
                        delegate(m);
                        changed = true;
                        fixed++;
                    } else {
                        leftovers.add(cn.name + "." + m.name + m.desc);
                    }
                }
                if (changed) {
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    cn.accept(cw);
                    bytes = cw.toByteArray();
                }
                out.putNextEntry(new ZipEntry(name));
                out.write(bytes);
                out.closeEntry();
            }
        }
        System.out.println("   StripIndy " + new File(args[0]).getName() + ": sửa " + fixed + " hàm, bỏ " + dropped + " lớp");
        if (!leftovers.isEmpty()) {
            System.err.println("Còn invokedynamic chưa xử lý: " + leftovers);
            System.exit(2);
        }
    }

    static boolean hasIndy(MethodNode m) {
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
            if (i.getOpcode() == Opcodes.INVOKEDYNAMIC) return true;
        return false;
    }

    /** Thân mới: nạp toàn bộ tham số, gọi hàm cùng tên/chữ ký ở Comparators, trả kết quả. */
    static void delegate(MethodNode m) {
        InsnList body = new InsnList();
        int slot = 0;
        for (Type t : Type.getArgumentTypes(m.desc)) {
            body.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
            slot += t.getSize();
        }
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, m.name, m.desc, false));
        body.add(new InsnNode(Type.getReturnType(m.desc).getOpcode(Opcodes.IRETURN)));
        m.instructions = body;
        m.tryCatchBlocks = new ArrayList<>();
        m.localVariables = null;
        m.visibleLocalVariableAnnotations = null;
        m.invisibleLocalVariableAnnotations = null;
    }
}
