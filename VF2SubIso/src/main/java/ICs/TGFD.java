package ICs;

import Infra.DataDependency;
import Infra.Delta;
import Infra.VF2PatternGraph;

public class TGFD {

    private Delta delta;
    private DataDependency dependency;
    private VF2PatternGraph pattern;
    private String name="";

    public TGFD (VF2PatternGraph pattern, Delta delta, DataDependency dependency, String name)
    {
        this.delta=delta;
        this.pattern=pattern;
        this.dependency=dependency;
        this.name=name;
    }

    public TGFD ()
    {
        this.dependency=new DataDependency();
    }

    public void setDelta(Delta delta) {
        this.delta = delta;
    }

    public void setDependency(DataDependency dependency) {
        this.dependency = dependency;
    }

    public void setPattern(VF2PatternGraph pattern) {
        this.pattern = pattern;
    }

    public VF2PatternGraph getPattern() {
        return pattern;
    }

    public DataDependency getDependency() {
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
                "delta =" + delta +
                ", dependency =" + dependency +
                ", pattern =" + pattern +
                '}';
    }
}
