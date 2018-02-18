package com.github.nfalco79.junit4osgi.runner.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

public class Report {

	/* package */ enum FailureType {
		IGNORE, FAILURE, ERROR, SUCCESS
	}

	private final Description description;
	private String message;
	private double elapsedTime = 0d;
	private Failure failure;
	private String err;
	private String out;
	private FailureType type = FailureType.SUCCESS;
	private int runCount;

	private Collection<Report> runs = new LinkedList<Report>();
	private List<Report> children = new LinkedList<Report>();

	public Report(Description description) {
		this.description = description;
	}

	public Description getDescription() {
		return description;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Failure getFailure() {
		return failure;
	}

	public void setFailure(Failure failure) {
		this.failure = failure;
		type = ReportListener.isFailure(failure) ? FailureType.FAILURE : FailureType.ERROR;
	}

	public double getElapsedTime() {
		return elapsedTime;
	}

	public void setElapsedTime(double elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	@Override
	public String toString() {
		return description + " " + type;
	}

	public String getErr() {
		return err;
	}

	public void setErr(String err) {
		this.err = err;
	}

	public String getOut() {
		return out;
	}

	public void setOut(String out) {
		this.out = out;
	}

	public List<Report> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public void addChild(Report child) {
		this.children.add(child);
	}

	public Collection<Report> getRuns() {
		return Collections.unmodifiableCollection(runs);
	}

	public void addRun(Report run) {
		runs.add(run);
		runCount = runs.size();
	}

	public int getRunCount() {
		return runCount;
	}

	public void setRunCount(int runCount) {
		this.runCount = runCount;
	}

	public FailureType getType() {
		return type;
	}

	public void markAsIgnored() {
		this.type = FailureType.IGNORE;
	}

	public boolean isSuccess() {
		final Iterator<Report> it = runs.iterator();
		while (it.hasNext()) {
			if (it.next().isSuccess()) {
				return true;
			}
		}
		return failure == null;
	}

}