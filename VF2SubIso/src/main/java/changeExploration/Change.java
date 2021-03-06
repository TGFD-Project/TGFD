package changeExploration;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** This is a base class for the changes  */
public class Change {

    //region --[Fields: Private]---------------------------------------

    /** Type of the change. */
    private ChangeType cType;

    /** The set of relevant TGFDs for each change based on the name of the TGFD */
    private Set <String> TGFDs=new HashSet <>();

    //endregion

    //region --[Constructors]------------------------------------
    public Change(ChangeType cType)
    {
        this.cType=cType;
    }

    //endregion

    //region --[Methods: Public]---------------------------------------

    /**
     * This method will add the relevant TGFD name for a change
     * @param TGFDName The name of the TGFD
     */
    public void addTGFD(String TGFDName)
    {
        this.TGFDs.add(TGFDName);
    }

    /**
     * @param TGFDNames Set of TGFD names to be added for a change
     */
    public void addTGFD(Collection <String> TGFDNames)
    {
        this.TGFDs.addAll(TGFDNames);
    }

    //endregion

    //region --[Properties: Public]------------------------------------

    /** Gets the change type. */
    public ChangeType getTypeOfChange() {
        return cType;
    }

    /** Gets the name of the set of of relevant TGFDs. */
    public Set <String> getTGFDs() {
        return TGFDs;
    }

    //endregion
}
