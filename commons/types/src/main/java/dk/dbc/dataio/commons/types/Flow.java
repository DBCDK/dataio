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
     private static final long serialVersionUID = -8809513217759455225L;

     private /* final */ long id;
     private /* final */ long version;
     private /* final */ FlowContent content;

     private Flow() { }

     /**
      * Class constructor
      *
      * @param id flow id
      * @param version flow version
      * @param content flow content
      *
      * @throws NullPointerException if given null-valued content
      */
     public Flow(long id, long version, FlowContent content) {
         this.id = id;
         this.version = version;
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
