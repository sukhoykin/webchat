package name.sukhoykin.cryptic.command;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class CommandDecoder implements Decoder.Text<CommandMessage> {

    private Gson gson;

    @Override
    public void init(EndpointConfig config) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(CommandMessage.class, new CommandDeserializer());
        gson = builder.create();
    }

    @Override
    public void destroy() {
    }

    @Override
    public CommandMessage decode(String s) throws DecodeException {

        try {
            return gson.fromJson(s, CommandMessage.class);
        } catch (JsonParseException e) {
            throw new DecodeException(s, e.getMessage(), e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }
}