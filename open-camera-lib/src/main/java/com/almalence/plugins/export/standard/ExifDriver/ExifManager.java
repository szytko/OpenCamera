package com.almalence.plugins.export.standard.ExifDriver;

import java.util.Arrays;

import android.content.Context;

import com.almalence.plugins.export.standard.ExifDriver.Values.ExifValue;
import com.almalence.plugins.export.standard.ExifDriver.Values.UndefinedValueAccessException;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueByteArray;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueNumber;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueRationals;

public class ExifManager
{
	ExifDriver	driver;
	Context		context;

	public ExifManager(ExifDriver _driver, Context _context)
	{
		driver = _driver;
		context = _context;
	}

	/*
	 * Here go the user-space set/get methods
	 */
	/**
	 * Get photographer/editor copyright pair string. In case, that only editor
	 * is specified, photographer copyright will contain space character, so it
	 * is recommended to trim it, or use specialized getPhotographerCopyright()
	 * method
	 * 
	 * @return Array of Strings. Item 0 is photographer copyright, 1 holds the
	 *         editor copyright
	 */
	public String[] getCopyright()
	{
		byte[][] result = new byte[2][];
		result[0] = new byte[] { 0 };
		result[1] = new byte[] { 0 };
		ExifValue exifValue = driver.getIfd0().get(ExifDriver.TAG_COPYRIGHT);
		if (exifValue != null && exifValue.getDataType() == ExifDriver.FORMAT_UNDEFINED)
		{
			byte[] values;
			try
			{
				values = exifValue.getBytes();
			} catch (UndefinedValueAccessException e)
			{
				e.printStackTrace();
				return new String[] { "Error" };
			}
			int copyrightIndex = 0;
			result[0] = new byte[values.length];
			Arrays.fill(result[0], (byte) 0);
			result[1] = new byte[values.length];
			Arrays.fill(result[1], (byte) 0);
			int index = 0;
			for (int i = 0; i < values.length && copyrightIndex < 2; i++)
			{
				if (values[i] != 0)
				{
					result[copyrightIndex][index] = values[i];
					index++;
				} else
				{
					copyrightIndex++;
					index = 0;
				}
			}
		}
		return new String[] { new String(result[0]).trim(), new String(result[1]).trim() };
	}


	private int[][] toDdMmSs(double _value)
	{
		double value = Math.abs(_value);
		int[][] ddmmss = new int[3][2];
		ddmmss[0][0] = (int) Math.floor(value);
		ddmmss[0][1] = 1;
		value -= Math.floor(value);
		value *= 60;
		ddmmss[1][0] = (int) Math.floor(value);
		ddmmss[1][1] = 1;
		value -= Math.floor(value);
		value *= 60000;
		ddmmss[2][0] = (int) Math.floor(value);
		ddmmss[2][1] = 1000;
		return ddmmss;
	}

	private void setGpsVersion()
	{
		ValueNumber version = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_BYTE);
		version.setIntegers(new int[] { 2, 2, 0, 0 });
		driver.getIfdExif().put(ExifDriver.TAG_GPS_VERSION_ID, version);
	}

	public void setGPSLocation(double _lat, double _lon, double _alt)
	{
		setGpsVersion();
		// Latitude
		ValueByteArray latRef = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
		ValueRationals lat = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
		lat.setRationals(toDdMmSs(_lat));
		if (_lat > 0)
		{
			latRef.setBytes(new byte[] { 'N' });
		} else
		{
			latRef.setBytes(new byte[] { 'S' });
		}
		driver.getIfdGps().put(ExifDriver.TAG_GPS_LATITUDE, lat);
		driver.getIfdGps().put(ExifDriver.TAG_GPS_LATITUDE_REF, latRef);
		// Longitude
		ValueByteArray lonRef = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
		ValueRationals lon = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
		lon.setRationals(toDdMmSs(_lon));
		if (_lon > 0)
		{
			lonRef.setBytes(new byte[] { 'E' });
		} else
		{
			lonRef.setBytes(new byte[] { 'W' });
		}
		driver.getIfdGps().put(ExifDriver.TAG_GPS_LONGITUDE, lon);
		driver.getIfdGps().put(ExifDriver.TAG_GPS_LONGITUDE_REF, lonRef);
		// Altitude
		ValueNumber altRef = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_BYTE);
		ValueRationals alt = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
		int[][] altValue = new int[1][];
		altValue[0] = new int[] { (int) Math.abs(_alt), 1 };
		alt.setRationals(altValue);
		if (_alt >= 0)
		{
			altRef.setIntegers(new int[] { 0 });
		} else
		{
			altRef.setIntegers(new int[] { 1 });
		}
		driver.getIfdGps().put(ExifDriver.TAG_GPS_ALTITUDE, alt);
		driver.getIfdGps().put(ExifDriver.TAG_GPS_ALTITUDE_REF, altRef);
	}

	// Convert string like "123/456" or "1.23" to Rational (2 integers).
	public static int[][] stringToRational(String string)
	{
		int[][] res = null;
		String[] splited = string.split("/");
		if (splited.length == 2)
		{
			res = new int[1][2];
			res[0][0] = Integer.parseInt(splited[0]);
			res[0][1] = Integer.parseInt(splited[1]);
			return res;
		}

		splited = string.split("\\.");
		if (splited.length == 2)
		{
			res = new int[1][2];
			res[0][0] = Integer.parseInt(splited[0] + splited[1]);
			res[0][1] = 10;
			for (int i = 0; i < splited[1].length() - 1; i++)
			{
				res[0][1] *= 10;
			}
			return res;
		}

		return res;
	}
}
