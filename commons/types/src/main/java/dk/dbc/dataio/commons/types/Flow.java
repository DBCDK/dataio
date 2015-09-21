/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

 /**
 * Flow DTO class.
 */
 public class Flow implements Serializable {
     private static final long serialVersionUID = -8809513217759455225L;

     private final long id;
     private final long version;
     private final FlowContent content;

     /**
      * Class constructor
      *
      * @param id flow id (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_ID_LOWER_BOUND})
      * @param version flow version (larger than or equal to {@value dk.dbc.dataio.commons.types.Constants#PERSISTENCE_VERSION_LOWER_BOUND})
      * @param content flow content
      *
      * @throws NullPointerException if given null-valued content
      * @throws IllegalArgumentException if value of id or version is not larger than or equal to lower bound
      */
     @JsonCreator
     public Flow(@JsonProperty("id") long id,
                 @JsonProperty("version") long version,
                 @JsonProperty("content") FlowContent content) {

         this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.PERSISTENCE_ID_LOWER_BOUND);
         this.version = InvariantUtil.checkLowerBoundOrThrow(version, "version", Constants.PERSISTENCE_VERSION_LOWER_BOUND);
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

     @JsonIgnore
     public boolean hasNextComponents() {
         for (FlowComponent flowComponent : content.getComponents()) {
             if (flowComponent.getNext() != FlowComponent.UNDEFINED_NEXT) {
                 return true;
             }
         }
         return false;
     }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (!(o instanceof Flow)) return false;

         Flow flow = (Flow) o;

         if (id != flow.id) return false;
         if (version != flow.version) return false;
         return content.equals(flow.content);

     }

     @Override
     public int hashCode() {
         int result = (int) (id ^ (id >>> 32));
         result = 31 * result + (int) (version ^ (version >>> 32));
         result = 31 * result + content.hashCode();
         return result;
     }
 }
