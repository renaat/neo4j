/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.ext;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.Description;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.helpers.Service;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.lifecycle.LifeSupport;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.logging.Log;
import org.neo4j.ndp.runtime.Sessions;
import org.neo4j.ndp.runtime.internal.StandardSessions;
import org.neo4j.ndp.transport.socket.SocketTransport;

import static org.neo4j.helpers.Settings.BOOLEAN;
import static org.neo4j.helpers.Settings.HOSTNAME_PORT;
import static org.neo4j.helpers.Settings.setting;

/**
 * Wraps NDP and exposes it as a Kernel Extension.
 */
@Service.Implementation(KernelExtensionFactory.class)
public class NDPKernelExtension extends KernelExtensionFactory<NDPKernelExtension.Dependencies>
{
    public static class Settings
    {
        @Description( "Max time that sessions can be idle, after this interval a session will get closed." )
        public static final Setting<Boolean> ndp_enabled = setting("experimental.ndp.enabled", BOOLEAN,
                "false" );

        @Description( "Host and port for the Neo4j Data Protocol http transport" )
        public static final Setting<HostnamePort> ndp_address =
                setting("dbms.ndp.address", HOSTNAME_PORT, "localhost:7687" );
    }

    public interface Dependencies
    {
        LogService logService();
        Config config();
        GraphDatabaseService db();
    }

    public NDPKernelExtension()
    {
        super( "neo4j-data-protocol-server" );
    }

    @Override
    public Lifecycle newKernelExtension( Dependencies dependencies ) throws Throwable
    {
        final Config config = dependencies.config();
        final GraphDatabaseService gdb = dependencies.db();
        final GraphDatabaseAPI api = (GraphDatabaseAPI) gdb;
        final Log log = dependencies.logService().getInternalLog( Sessions.class );
        final HostnamePort address = config.get( Settings.ndp_address );
        final LifeSupport life = new LifeSupport();

        if(config.get( Settings.ndp_enabled ))
        {
            final Sessions env = life.add( new StandardSessions( api, log ) );

            // Start services
            life.add( new SocketTransport( address, log, env ) );
            log.info( "NDP Server extension loaded." );
        }

        return life;
    }
}
