package org.nrg.xnat.helpers.prearchive;

import static org.junit.Assert.*;

import java.util.Calendar;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PrearcSchedulerTest {
	@Test
	public final void testDiffInMinutes() {
		long now = Calendar.getInstance().getTimeInMillis();
		long then = now - (1000*60*6);
		double minutes = PrearcScheduler.diffInMinutes(then, now);
		Assert.assertEquals(6.0, minutes);
	}
}
