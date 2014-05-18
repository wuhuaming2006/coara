/*
 * LipeRMI - a light weight Internet approach for remote method invocation
 * Copyright (C) 2006  Felipe Santos Andrade
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * For more information, see http://lipermi.sourceforge.net/license.php
 * You can also contact author through lipeandrade@users.sourceforge.net
 */

package net.sf.lipermi.handler.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.sf.lipermi.call.IRemoteMessage;

import org.apache.log4j.Logger;


/**
 * GZip filter to compact data using GZip I/O streams.
 *
 * @author lipe
 * @date   07/10/2006
 *
 * @see net.sf.lipermi.handler.filter.DefaultFilter
 */
public class GZipFilter implements IProtocolFilter {
	static Logger log = Logger.getLogger(GZipFilter.class.getName());
	
	Class<? extends ObjectInputStream> objectInputStreamClass;
	Class<? extends ObjectOutputStream> objectOutputStreamClass;
	
    public GZipFilter(Class<? extends ObjectInputStream> input,
			Class<? extends ObjectOutputStream> output) {
    	this.objectInputStreamClass = input;
    	this.objectOutputStreamClass = output;
	}
    
    public GZipFilter() {
    	this.objectInputStreamClass = ObjectInputStream.class;
    	this.objectOutputStreamClass = ObjectOutputStream.class;
    }

	public IRemoteMessage readObject(Object obj) {
        IRemoteMessage remoteMessage = null;
        GZIPInputStream gzis = null;
        ObjectInputStream ois = null;

        try {
        	Date now = new Date();
        	Date now2 = new Date();
        	log.trace("readObject starting...");
            ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) obj);
            gzis = new GZIPInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int sChunk = 8192;
            log.trace("sChunk: " + sChunk);
            byte[] buffer = new byte[sChunk];
            int length;
            while ((length = gzis.read(buffer, 0, sChunk)) != -1) {
                    baos.write(buffer, 0, length);
            }

            gzis.close();
            byte[] extractedObj = baos.toByteArray();
            log.trace("unzip time: " + (((new Date()).getTime()) - now2.getTime()) + " ms");
            log.trace("extractedObj size: " + extractedObj.length);
            now2 = new Date();
            bais = new ByteArrayInputStream(extractedObj);            
            ois = objectInputStreamClass.getConstructor(InputStream.class).newInstance(bais);
            remoteMessage = (IRemoteMessage) ois.readUnshared();
            log.trace("unserialize time: " + (((new Date()).getTime()) - now2.getTime()) + " ms");
            ois.close();
            log.trace("readObject total time: " + (((new Date()).getTime()) - now.getTime()) + " ms");
        }
        catch (Exception e) {
            throw new RuntimeException("Can't read message", e); //$NON-NLS-1$
        }
        finally {
            if (gzis != null)
                try {
                    gzis.close();
                } catch (IOException e) {}

            if (ois != null)
                try {
                    ois.close();
                } catch (IOException e) {}
        }
        return remoteMessage;
    }



public Object prepareWrite(IRemoteMessage message) {
    Object objectToWrite = message;

    ObjectOutputStream oos = null;
    GZIPOutputStream gzos = null;
    try {
        // serialize obj
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        oos = objectOutputStreamClass.getConstructor(OutputStream.class).newInstance(baos);
        // oos.reset(); -- not needed here because the oos i
        //                 always a new instance, reseted.
        Date now = new Date();
        oos.writeUnshared(message);
        byte[] byteObj = baos.toByteArray();
        log.trace("Serialize time: " + (((new Date()).getTime()) - now.getTime()) + " ms");
        log.trace("Message size before Zip: " + byteObj.length + " bytes");

        baos.reset();

        // compact the serialization
        now = new Date();
        baos = new ByteArrayOutputStream(byteObj.length);
        gzos = new GZIPOutputStream(baos, byteObj.length) {{def.setLevel(1);}};
        gzos.write(byteObj);
        gzos.finish();
        byteObj = baos.toByteArray();
        log.trace("Zip time: " + (((new Date()).getTime()) - now.getTime()) + " ms");
        log.trace("Message size after Zip: " + byteObj.length + " bytes");
        objectToWrite = byteObj;
    }
    catch (Exception e) {
        throw new RuntimeException("Can't prepare message", e); //$NON-NLS-1$
    }
    finally {
        if (gzos != null)
            try {
                gzos.close();
            } catch (IOException e) {}

        if (oos != null)
            try {
                oos.close();
            } catch (IOException e) {}
    }
    return objectToWrite;
}
}


// vim: ts=4:sts=4:sw=4:expandtab
