package infra;

public class TGFD {

    private Interval delta;
    private Dependency dependency;
    private VF2PatternGraph pattern;

    public TGFD (VF2PatternGraph pattern, Interval delta, Dependency dependency)
    {
        this.delta=delta;
        this.pattern=pattern;
        this.dependency=dependency;
    }

    public VF2PatternGraph getPattern() {
        return pattern;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public Interval getDelta() {
        return delta;
    }
}
