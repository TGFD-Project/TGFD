package changeExploration;


import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** This is a base class for the changes  */
public class Change implements Serializable {

    //region --[Fields: Private]---------------------------------------

    /** Type of the change. */
    private ChangeType cType;

    /** The set of relevant TGFDs for each change based on the name of the TGFD */
    private Set <String> TGFDs=new HashSet <>();

    /** Unique id of a change log. */
    private int id;
    //endregion

    /**
     * @param cType Type of the change
     * @param id unique id of the change log
     */
    //region --[Constructors]------------------------------------
    public Change(ChangeType cType, int id)
    {
        this.cType=cType;
        this.id=id;
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
        if(TGFDNames!=null) {
            this.TGFDs.addAll(TGFDNames);
        }
    }

    /**
     * @param TGFDNames Set of TGFD names to be added for a change
     */
    public void addTGFD(Stream <String> TGFDNames)
    {
        this.TGFDs.addAll(TGFDNames.collect(Collectors.toSet()));
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

    /** Gets the id of the change. */
    public int getId() {
        return id;
    }

    //endregion
}
