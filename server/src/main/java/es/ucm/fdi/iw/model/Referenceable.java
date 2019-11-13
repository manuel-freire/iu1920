package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import javax.persistence.Transient;
import java.io.IOException;
import java.util.List;

public abstract class Referenceable {
    @Transient
    public abstract String getRef();

    public static class RefSerializer<T extends Referenceable> extends JsonSerializer<T> {
        @Override
        public void serialize(T r, JsonGenerator g, SerializerProvider serializerProvider)
                throws IOException, JsonProcessingException {
            g.writeString(r.getRef());
        }
    }

    public static class ListSerializer<T extends Referenceable> extends JsonSerializer<List<T>> {
        @Override
        public void serialize(List<T> rs, JsonGenerator g, SerializerProvider serializerProvider)
                throws IOException, JsonProcessingException {
            g.writeStartArray();
            for (Referenceable r : rs) g.writeObject(r.getRef());
            g.writeEndArray();
        }
    }

    public static class StringToListSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String ss, JsonGenerator g, SerializerProvider serializerProvider)
                throws IOException, JsonProcessingException {
            g.writeStartArray();
            for (String s : ss.split(",")) g.writeString(s);
            g.writeEndArray();
        }
    }
}
