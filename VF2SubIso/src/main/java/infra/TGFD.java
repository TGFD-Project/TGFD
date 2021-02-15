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

    public TGFD ()
    {
        this.dependency=new Dependency();
    }

    public void setDelta(Delta delta) {
        this.delta = delta;
    }

    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }

    public void setPattern(VF2PatternGraph pattern) {
        this.pattern = pattern;
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
