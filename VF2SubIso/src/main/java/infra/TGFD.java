package infra;

public class TGFD {

    private Delta delta;
    private Dependency dependency;
    private VF2PatternGraph pattern;
    private String name="";

    public TGFD (VF2PatternGraph pattern, Delta delta, Dependency dependency,String name)
    {
        this.delta=delta;
        this.pattern=pattern;
        this.dependency=dependency;
        this.name=name;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TGFD{" +
                "delta=" + delta +
                ", dependency=" + dependency +
                ", pattern=" + pattern +
                '}';
    }
}
