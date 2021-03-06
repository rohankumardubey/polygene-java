/*
 * Copyright (c) 2010-2012, Paul Merlin. All Rights Reserved.
 * Copyright (c) 2012, Niclas Hedhman. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *     You may obtain a copy of the License at 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.library.scheduler.schedule.once;

import java.time.Instant;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.library.scheduler.schedule.Schedule;

@Mixins( OnceSchedule.OnceScheduleMixin.class )
public interface OnceSchedule
    extends Schedule
{
    abstract class OnceScheduleMixin
        implements OnceSchedule
    {
        private boolean running;

        @Override
        public void taskStarting()
        {
            running = true;
        }

        @Override
        public void taskCompletedSuccessfully()
        {
            running = false;
        }

        @Override
        public void taskCompletedWithException( RuntimeException ex )
        {
            running = false;
        }

        @Override
        public boolean isTaskRunning()
        {
            return running;
        }

        @Override
        public Instant nextRun( Instant from )
        {
            if( ! start().get().isBefore(from) )
            {
                return start().get();
            }
            return Instant.MIN;
        }

        @Override
        public String presentationString()
        {
            return start().get().toString();
        }
    }

}
