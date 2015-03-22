/*
 * Copyright 2002-2012 Drew Noakes
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    http://drewnoakes.com/code/exif/
 *    http://code.google.com/p/metadata-extractor/
 */
package com.almalence.util.exifreader.imaging.jpeg;

import com.almalence.util.exifreader.lang.annotations.NotNull;
import com.almalence.util.exifreader.lang.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Holds a collection of Jpeg data segments.  This need not necessarily be all segments
 * within the Jpeg.  For example, it may be convenient to store only the non-image
 * segments when analysing (or serializing) metadata.
 * <p/>
 * Segments are keyed via their segment marker (a byte).  Where multiple segments use the
 * same segment marker, they will all be stored and available.
 *
 * @author Drew Noakes http://drewnoakes.com
 */
public class JpegSegmentData implements Serializable
{
    private static final long serialVersionUID = 7110175216435025451L;
    
    /** A map of byte[], keyed by the segment marker */
    @NotNull
    private final HashMap<Byte, List<byte[]>> _segmentDataMap = new HashMap<Byte, List<byte[]>>(10);

    /**
     * Adds segment bytes to the collection.
     * @param segmentMarker
     * @param segmentBytes
     */
    @SuppressWarnings({ "MismatchedQueryAndUpdateOfCollection" })
    public void addSegment(byte segmentMarker, @NotNull byte[] segmentBytes)
    {
        final List<byte[]> segmentList = getOrCreateSegmentList(segmentMarker);
        segmentList.add(segmentBytes);
    }

    /**
     * Gets the first Jpeg segment data for the specified marker.
     * @param segmentMarker the byte identifier for the desired segment
     * @return a byte[] containing segment data or null if no data exists for that segment
     */
    @Nullable
    public byte[] getSegment(byte segmentMarker)
    {
        return getSegment(segmentMarker, 0);
    }

    /**
     * Gets segment data for a specific occurrence and marker.  Use this method when more than one occurrence
     * of segment data for a given marker exists.
     * @param segmentMarker identifies the required segment
     * @param occurrence the zero-based index of the occurrence
     * @return the segment data as a byte[], or null if no segment exists for the marker & occurrence
     */
    @Nullable
    public byte[] getSegment(byte segmentMarker, int occurrence)
    {
        final List<byte[]> segmentList = getSegmentList(segmentMarker);

        if (segmentList==null || segmentList.size()<=occurrence)
            return null;
        else
            return segmentList.get(occurrence);
    }

    /**
     * Returns all instances of a given Jpeg segment.  If no instances exist, an empty sequence is returned.
     *
     * @param segmentMarker a number which identifies the type of Jpeg segment being queried
     * @return zero or more byte arrays, each holding the data of a Jpeg segment
     */
    @NotNull
    public Iterable<byte[]> getSegments(byte segmentMarker)
    {
        final List<byte[]> segmentList = getSegmentList(segmentMarker);
        return segmentList==null ? new ArrayList<byte[]>() : segmentList;
    }

    @Nullable
    public List<byte[]> getSegmentList(byte segmentMarker)
    {
        return _segmentDataMap.get(Byte.valueOf(segmentMarker));
    }

    @NotNull
    private List<byte[]> getOrCreateSegmentList(byte segmentMarker)
    {
        List<byte[]> segmentList;
        if (_segmentDataMap.containsKey(segmentMarker)) {
            segmentList = _segmentDataMap.get(segmentMarker);
        } else {
            segmentList = new ArrayList<byte[]>();
            _segmentDataMap.put(segmentMarker, segmentList);
        }
        return segmentList;
    }

}
