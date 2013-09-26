package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

 /**
 * Flow DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
 public class Flow implements Serializable {
     static /* final */ long ID_VERSION_LOWER_THRESHOLD = 0;

     private static final long serialVersionUID = -8809513217759455225L;

     private /* final */ long id;
     private /* final */ long version;
     private /* final */ FlowContent content;

     private Flow() { }

     /**
      * Class constructor
      *
      * Attention: when changing the signature of this constructor
      * remember to also change the signature in the corresponding *JsonMixIn class.
      *
      * @param id flow id (> {@value #ID_VERSION_LOWER_THRESHOLD})
      * @param version flow version (> {@value #ID_VERSION_LOWER_THRESHOLD})
      * @param content flow content
      *
      * @throws NullPointerException if given null-valued content
      * @throws IllegalArgumentException if value of id or version is not above {@value #ID_VERSION_LOWER_THRESHOLD}
      */
     public Flow(long id, long version, FlowContent content) {
         this.id = InvariantUtil.checkAboveThresholdOrThrow(id, "id", ID_VERSION_LOWER_THRESHOLD);
         this.version = InvariantUtil.checkAboveThresholdOrThrow(version, "version", ID_VERSION_LOWER_THRESHOLD);
         this.content = InvariantUtil.checkNotNullOrThrow(content, "content");
     }

     public long getId() {
         return id;
     }

     public long getVersion() {
         return version;
     }

     public FlowContent getContent() {
         return content;
     }
 }
