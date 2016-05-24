from telnetlib import *
from random import *
import time
import re

server_encoding = 'UTF-8'

def log(*messages):
    print(name, ": ", *messages)

class ServerConnection:
    def __init__(self, host, port):
        self.telnet = Telnet(host, port)
        # self.telnet.set_debuglevel(4)

    def read_line(self):
        return self.telnet.read_until(b'\n').decode(server_encoding)

    def write(self, message):
        return self.telnet.write(bytes(message + '\n', encoding=server_encoding))

class GameMap:
    def __init__(self, str, map_size_x, map_size_y):
        self.str = str
        self.map_size_x = map_size_x
        self.map_size_y = map_size_y

    def __getitem__(self, pos):
        x, y = pos
        index = 2 * x + y * 2 * self.map_size_x
        return self.str[index]

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

current_map_str = ""
current_map = None

def select_possible_moves(position, moves):
    def move_possible(move):
        field = 0
        if move == 'w' and position[3] > 0:
            field = current_map[position[2], position[3] - 1]
        elif move == 'a' and position[2] > 0:
            field = current_map[position[2] - 1, position[3]]
        elif move == 's' and position[3] < map_size_y - 1:
            field = current_map[position[2], position[3] + 1]
        elif move == 'd' and position[2] < map_size_x - 1:
            field = current_map[position[2]  + 1, position[3]]

        return field != 'w' and field != 'W'

    return filter(move_possible, moves)

def select_first_possible_move(position, moves):
    return next(select_possible_moves(position, moves))

while ongoing:
    str = connection.read_line()

#    print(str[:-1])

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
        current_map_str = ''
    elif reading_map and str == '\n':
        current_map = GameMap(current_map_str, map_size_x, map_size_y)
        reading_map = False
    elif reading_map:
        current_map_str += str[:-1]

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

        is_fox = current_map[position_x, position_y] == 'f'

        scores.append((player_name, score, position_x, position_y, is_fox))

    elif str.startswith('wfyc'):
        time.sleep(0.01)

        my_score = next(filter(lambda score: score[0] == name, scores))
        fox_score = next(filter(lambda score: score[4], scores))

        if my_score == fox_score:
            action = choice(['w', 's', 'a', 'd'])
            log('I am the fox. My turn is ' + action)

            connection.write(action)
        else:
            delta_x = my_score[2] - fox_score[2]
            delta_y = my_score[3] - fox_score[3]

            moves = []
            if abs(delta_x) > abs(delta_y):
                moves.append('a' if delta_x > 0 else 'd')
                moves.append('w' if delta_y > 0 else 's')
                moves.append('d' if delta_x > 0 else 'a')
                moves.append('s' if delta_y > 0 else 'w')
            else:
                moves.append('w' if delta_y > 0 else 's')
                moves.append('a' if delta_x > 0 else 'd')
                moves.append('s' if delta_y > 0 else 'w')
                moves.append('d' if delta_x > 0 else 'a')

            action = select_first_possible_move(my_score, moves)
            log('my turn is ' + action)

            connection.write(action)

    # terminate the loop if game is over
    elif str.startswith('game is over'):
        ongoing = False
