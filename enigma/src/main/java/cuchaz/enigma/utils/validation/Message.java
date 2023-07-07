package cuchaz.enigma.utils.validation;

import cuchaz.enigma.utils.I18n;

public class Message {
	public static final Message EMPTY_FIELD = create(Type.ERROR, "empty_field");
	public static final Message INVALID_IP = create(Type.ERROR, "invalid_ip");
	public static final Message NOT_INT = create(Type.ERROR, "not_int");
	public static final Message FIELD_OUT_OF_RANGE_INT = create(Type.ERROR, "field_out_of_range_int");
	public static final Message FIELD_LENGTH_OUT_OF_RANGE = create(Type.ERROR, "field_length_out_of_range");
	public static final Message NON_UNIQUE_NAME_CLASS = create(Type.ERROR, "non_unique_name_class");
	public static final Message NON_UNIQUE_NAME = create(Type.ERROR, "non_unique_name");
	public static final Message ILLEGAL_IDENTIFIER = create(Type.ERROR, "illegal_identifier");
	public static final Message RESERVED_IDENTIFIER = create(Type.ERROR, "reserved_identifier");
	public static final Message ILLEGAL_DOC_COMMENT_END = create(Type.ERROR, "illegal_doc_comment_end");
	public static final Message UNKNOWN_RECORD_GETTER = create(Type.ERROR, "unknown_record_getter");
	public static final Message INVALID_PACKAGE_NAME = create(Type.ERROR, "invalid_package_name");

	public static final Message SHADOWED_NAME_CLASS = create(Type.WARNING, "shadowed_name_class");
	public static final Message SHADOWED_NAME = create(Type.WARNING, "shadowed_unique_name");

	public static final Message SERVER_STARTED = create(Type.INFO, "server_started");
	public static final Message CONNECTED_TO_SERVER = create(Type.INFO, "connected_to_server");
	public static final Message LEFT_SERVER = create(Type.INFO, "left_server");
	public static final Message MULTIPLAYER_USER_CONNECTED = create(Type.INFO, "user_connected");
	public static final Message MULTIPLAYER_USER_LEFT = create(Type.INFO, "user_left_server");
	public static final Message MULTIPLAYER_CHAT = new Message(Type.INFO, "", "");

	public static final Message OPENED_PROJECT = create(Type.INFO, "opened_project");
	public static final Message OPENED_JAR = create(Type.INFO, "opened_jar");

	private final Type type;
	private final String textKey;
	private final String longTextKey;

	private Message(Type type, String textKey, String longTextKey) {
		this.type = type;
		this.textKey = textKey;
		this.longTextKey = longTextKey;
	}

	public Type getType() {
		return this.type;
	}

	public String format(Object[] args) {
		return I18n.translateFormatted(this.textKey, args);
	}

	public String formatDetails(Object[] args) {
		return I18n.translateOrEmpty(this.longTextKey, args);
	}

	public static Message create(Type type, String name) {
		return new Message(type, String.format("validation.message.%s", name), String.format("validation.message.%s.long", name));
	}

	@Override
	public String toString() {
		return this.textKey + " (" + this.type + ")";
	}

	public enum Type {
		INFO,
		WARNING,
		ERROR,
	}
}
