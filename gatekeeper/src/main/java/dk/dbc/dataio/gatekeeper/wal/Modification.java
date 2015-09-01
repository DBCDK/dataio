package dk.dbc.dataio.gatekeeper.wal;

import dk.dbc.dataio.gatekeeper.operation.Opcode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;

@Entity
public class Modification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Lob
    @Column(nullable = false)
    private String transfilePath;

    @Column(nullable = false)
    private Opcode opcode;

    @Lob
    @Column(nullable = false)
    private String arg;

    @Column(nullable = false)
    private Boolean locked;

    public Modification() {}

    public String getTransfilePath() {
        return transfilePath;
    }

    public void setTransfilePath(String transfilePath) {
        this.transfilePath = transfilePath;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public void setOpcode(Opcode opcode) {
        this.opcode = opcode;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public Boolean isLocked() {
        return locked;
    }

    public void lock() {
        this.locked = true;
    }

    @PrePersist
    void preInsert() {
        if (locked == null) {
            locked = false;
        }
    }

    @Override
    public String toString() {
        return "Modification{" +
                "id=" + id +
                ", transfilePath='" + transfilePath + '\'' +
                ", opcode=" + opcode +
                ", arg='" + arg + '\'' +
                ", locked=" + locked +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Modification that = (Modification) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Package scoped constructor used for unit testing
     * @param id injected id
     */
    Modification(Long id) {
        this.id = id;
        this.locked = false;
    }
}
