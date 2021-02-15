package infra;

public class TGFD {

    private Delta delta;
    private Dependency dependency;
    private VF2PatternGraph pattern;

    public TGFD (VF2PatternGraph pattern, Delta delta, Dependency dependency)
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

    public Delta getDelta() {
        return delta;
    }
}
