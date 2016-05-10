from telnetlib import *
from random import *
import time
import re

server_encoding = 'UTF-8'

class ServerConnection:
    def __init__(self, host, port):
        self.telnet = Telnet(host, port)
        # self.telnet.set_debuglevel(4)

    def read_line(self):
        return self.telnet.read_until(b'\n').decode(server_encoding)

    def write(self, message):
        return self.telnet.write(bytes(message + '\n', encoding=server_encoding))

def log(*messages):
    print(name, ": ", *messages)

name = choice(['Graham', 'John', 'Terry', 'Eric', 'Terry', 'Michael']) + ' ' + choice(['Chapman', 'Cleese', 'Gilliam', 'Idle', 'Jones', 'Palin'])
print('Hello my name is', name)

connection = ServerConnection('localhost', 5000)

while True:
    str = connection.read_line()
    if str.startswith('YourName'):
        connection.write('name:' + name)
        break

reading_map = False
reading_scoretable = False
ongoing = True

current_game_number = 0
total_game_number = 0
round_number = 0
total_rounds = 0
players_count = 0
map_size_x = 0
map_size_y = 0
timeout = 0.0

scores = []

current_map = None
while ongoing:
    str = connection.read_line()

    print(str[:-1])

    # reading general game info
    if str.startswith('game:'):
        match = re.search("game:([0-9]+)/([0-9]+),round:([0-9]+)/([0-9]+),players:([0-9]+),mapsize:x([0-9]+)y([0-9]+),timeout:([0-9]+\.[0-9]+)s,", str)

        current_game_number = int(match.group(1))
        total_game_number = int(match.group(2))

        round_number = int(match.group(3))
        total_rounds = int(match.group(4))

        players_count = int(match.group(5))

        map_size_x = int(match.group(6))
        map_size_y = int(match.group(7))

        timeout = float(match.group(8))

    # reading the map
    elif str.startswith('map:'):
        reading_map = True
        current_map = ''
    elif reading_map and str == '\n':
        reading_map = False
    elif reading_map:
        current_map += str[:-1]

    # reading the scoretable
    elif str.startswith('scoretable:'):
        scores = []
        reading_scoretable = True
    elif str.startswith('/scoretable'):
        reading_scoretable = False
    elif reading_scoretable:
        match = re.search('name:(.+),score:([0-9]+),x:([0-9]+),y:([0-9]+);', str)

        player_name = match.group(1)
        score = int(match.group(2))
        position_x = int(match.group(3))
        position_y = int(match.group(4))

        scores.append((player_name, score, position_x, position_y))

    # determine and send the next move
    elif str.startswith('wfyc'):
        time.sleep(0.01)

        action = choice(['w', 's', 'a', 'd', '', 'n'])
        direction = ''
        if action == '':
            direction = choice(['w', 's', 's', 'd'])

        log('my turn is ' + action + direction)

        connection.write(action + direction)

    # terminate the loop if game is over
    elif str.startswith('game is over'):
        ongoing = False
