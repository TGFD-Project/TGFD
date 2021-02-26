package changeExploration;

public class Change {

    private ChangeType cType;

    public Change(ChangeType cType)
    {
        this.cType=cType;
    }

    public ChangeType getTypeOfChange() {
        return cType;
    }
}
