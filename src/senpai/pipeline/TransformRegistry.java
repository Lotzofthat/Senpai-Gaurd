package senpai.pipeline;

import java.util.ArrayList;
import java.util.List;
import senpai.config.SenpaiConfig;
import senpai.transforms.flow.ControlFlowFlattener;
import senpai.transforms.flow.OpaquePredicateInjector;
import senpai.transforms.junk.DeadBranchInjector;
import senpai.transforms.junk.FakeSwitchInjector;
import senpai.transforms.numbers.NumericConstantMasker;
import senpai.transforms.rename.ClassRenamer;
import senpai.transforms.rename.FieldRenamer;
import senpai.transforms.rename.LocalVariableRenamer;
import senpai.transforms.rename.MethodRenamer;
import senpai.transforms.strings.StringEncryptor;

public final class TransformRegistry {

    private final SenpaiConfig cfg;

    public TransformRegistry(SenpaiConfig cfg) {
        this.cfg = cfg;
    }

    public List<Transform> build() {
        // ordering matters. rename of classes first to settle internal names,
        // then the deferred member rename collectors, then the applier that
        // executes the SimpleRemapper. only after the class graph is stable
        // do we touch literal contents and control flow, since those passes
        // inject synthetic members and we want their owners final.
        List<Transform> ordered = new ArrayList<>();
        if (cfg.renaming.classes) {
            ordered.add(new ClassRenamer());
        }
        if (cfg.renaming.methods) {
            ordered.add(new MethodRenamer());
        }
        if (cfg.renaming.fields) {
            ordered.add(new FieldRenamer());
        }
        // the applier runs whether or not locals are renamed so the rename map
        // is consumed in either case.
        ordered.add(new LocalVariableRenamer());

        if (cfg.strings.encrypt) {
            ordered.add(new StringEncryptor());
        }
        if (cfg.numbers.mask) {
            ordered.add(new NumericConstantMasker());
        }
        if (cfg.flow.opaquePredicates) {
            ordered.add(new OpaquePredicateInjector());
        }
        if (cfg.flow.flatten) {
            ordered.add(new ControlFlowFlattener());
        }
        if (cfg.junk.fakeSwitches) {
            ordered.add(new FakeSwitchInjector());
        }
        if (cfg.junk.deadBranches) {
            ordered.add(new DeadBranchInjector());
        }
        return ordered;
    }
}
