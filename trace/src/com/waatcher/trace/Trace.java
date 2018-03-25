package com.waatcher.trace;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Trace {
	public enum Status {
		ERROR, OK, VALIDATION_ERROR, WARN
	}

	private String className;
	private long duration;
	private LocalDateTime endDateTime;
	@Builder.Default
	private List<Trace> items = new ArrayList<>();
	@Builder.Default
	private List<String> messages = new ArrayList<>();
	private String method;
	@Builder.Default
	private LocalDateTime startDateTime = LocalDateTime.now();
	@Builder.Default
	private Status status = Status.OK;

	public Trace(Object obj, String method) {
		this(obj.getClass().getName(), method);
	}

	public Trace(String className, String method) {
		startDateTime = LocalDateTime.now();
		this.method = method;
		this.className = className;
	}

	public void addItem(Trace item) {
		items.add(item);
	}

	public void addMessage(String format, Object... args) {
		if (args.length > 0) {
			messages.add(String.format(format, args));
		} else if (format == null) {
			messages.add("java.lang.NullPointerException");
		} else {
			messages.add(format);
		}
	}

	public Trace createChildTrace(Object obj, String method) {
		return this.createChildTrace(obj.getClass().getName(), method);
	}

	public Trace createChildTrace(String className, String method) {
		Trace trace = Trace.builder().className(className).method(method).build();
		items.add(trace);
		return trace;
	}

	public void finish() {
		endDateTime = LocalDateTime.now();

		duration = endDateTime.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli()
				- startDateTime.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli();
	}

	public long getDuration() {
		if (endDateTime == null) {
			finish();
		}
		return duration;
	}

	public Status getStatus() {
		if (Status.OK.equals(status)) {
			for (Trace trace : items) {
				if (!Status.OK.equals(trace.getStatus())) {
					status = trace.getStatus();
					break;
				}
			}
		}

		return status;
	}

	public boolean hasError() {
		return Status.ERROR.equals(getStatus());
	}

	public boolean hasWarning() {
		return Status.WARN.equals(getStatus());
	}
}