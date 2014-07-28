"""Generate a sample project with triggers"""

from AndroidCodeGenerator.generator import Generator
from AndroidCodeGenerator.sql_validator import SQLTester
from AndroidCodeGenerator.db_table import (Table, Column, ForeignKey, Unique,
                                           Trigger, Check)
from AndroidCodeGenerator.database_triggers import DatabaseTriggers

tables = []
triggers = []

tasklists = Table('tasklist')
tasklists.add_cols(Column('title').text.not_null.default("''"),
                   Column('updated').integer,
                   Column('listtype').text,
                   Column('sorting').text,
                   Column('deleted').integer.not_null.default(0),
                   # New fields
                   Column('ctime').timestamp.default_current_timestamp,
                   Column('mtime').timestamp.default_current_timestamp,
                   # GTask fields
                   Column('account').text,
                   Column('remoteid').text)
tasklists.add_constraints(Unique('account', 'remoteid').on_conflict_replace)

tables.append(tasklists)

tasks = Table('task')
tasks.add_cols(Column('title').text.not_null.default("''"),
               Column('note').text.not_null.default("''"),
               Column('completed').integer,
               Column('updated').integer,
               Column('due').integer,
               Column('locked').integer.not_null.default(0),
               Column('deleted').integer.not_null.default(0),
               # Positions
               Column('posleft').integer.not_null.default(1),
               Column('posright').integer.not_null.default(2),
               Column('tasklist').integer.not_null,
               # New fields
               Column('ctime').timestamp.default_current_timestamp,
               Column('mtime').timestamp.default_current_timestamp,
               # GTask fields
               Column('account').text,
               Column('remoteid').text)


tasks.add_constraints(ForeignKey('tasklist').references(tasklists.name)\
                      .on_delete_cascade,
                      Unique('account', 'remoteid').on_conflict_replace,
                      Check('posleft', '> 0'),
                      Check('posright', '> 1'))

tables.append(tasks)

log = Table('history')
log.add_cols(Column('taskid').integer,
             Column('title').text.not_null.default("''"),
             Column('note').text.not_null.default("''"),
             Column('deleted').integer.not_null.default(0),
             Column('updated').timestamp.not_null.default_current_timestamp)
log.add_constraints(ForeignKey('taskid').references(tasks.name)\
                    .on_delete_set_null)

tables.append(log)

for name in ['tr_up_history', 'tr_ins_history',
             'tr_del_history']:
    t = Trigger(name).temp.if_not_exists
    deleted = 'new.deleted'
    if 'up' in name:
        t.after.update_on(tasks.name)
    elif 'ins' in name:
        t.after.insert_on(tasks.name)
    else:
        t.before.delete_on(tasks.name)
        deleted = 1
    t.do_sql("INSERT INTO {} \
(taskid, title, note, deleted) \
VALUES (new._id, new.title, new.note, {})".format(log.name, deleted))
    triggers.append(t)

# Notifications
nots = Table('notification')
nots.add_cols(Column('time').integer,
             Column('permanent').integer.not_null.default(0),
             Column('taskid').integer,
             Column('repeats').integer.not_null.default(0),
             Column('locationname').text,
             Column('latitude').real,
             Column('longitude').real,
             Column('radius').real)
nots.add_constraints(ForeignKey('taskid').references(tasks.name)\
                    .on_delete_cascade)
tables.append(nots)

# Let's try to create the SQL
st = SQLTester()
st.add_tables(*tables)
st.add_triggers(*triggers)

st.test_create()

#g = Generator(path='./sample/src/com/example/appname/database/')
#g.add_tables(persons, log)
#g.add_triggers(trigger)
#g.write()
