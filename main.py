from math import *
from random import *
from telnetlib import *
import itertools
import re
import time

import numpy as np
import scipy.sparse.csgraph as ng

server_encoding = 'UTF-8'

current_time = lambda: int(round(time.time() * 1000))

inf = float('Inf')

def log(*messages):
    print(name, ": ", *messages)

class ServerConnection:
    def __init__(self, host, port):
        self.telnet = Telnet(host, port)
        # self.telnet.set_debuglevel(4)

    def read_line(self):
        return self.telnet.read_until(b'\n').decode(server_encoding)

    def write(self, message):
        if message:
            return self.telnet.write(bytes(message + '\n', encoding=server_encoding))

class GameMap:
    def __init__(self, str, map_size_x, map_size_y):
        self.str = str
        self.size_x = map_size_x
        self.size_y = map_size_y

    def __getitem__(self, pos):
        x, y = pos
        index = 2 * x + y * 2 * self.size_x
        return self.str[index]

    def is_move_possible(self, score, move):
        new_position = (score[2] + move[0], score[3] + move[1])

        result = 0 <= new_position[0] and new_position[0] < self.size_x and \
            0 <= new_position[1] and new_position[1] < self.size_y and \
            (self[new_position[0], new_position[1]] != 'w' and \
            self[new_position[0], new_position[1]] != 'W')

        return result

name = choice(['Graham', 'John', 'Terry', 'Eric', 'Terry', 'Michael']) + ' ' + choice(['Chapman', 'Cleese', 'Gilliam', 'Idle', 'Jones', 'Palin'])
print('Hello my name is', name)

#connection = ServerConnection('localhost', 5000)
connection = ServerConnection('172.26.6.191', 5000)

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

my_index = None

current_map_str = ""
current_map = None

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

        if player_name == name:
            my_index = len(scores) - 1

    elif str.startswith('wfyc'):
        possible_moves = {
            'n': (0, 0),
            'w': (0, -1),
            's': (0, 1),
            'a': (-1, 0),
            'd': (1, 0)
        }

        possible_moves_keys = list(possible_moves.keys())

        timeout = 1000 * 0.75
        start_time = current_time()

        def distance(score, another_score):
            return sqrt((score[2] - another_score[2])**2) + sqrt((score[3] - another_score[3])**2)

        def rate(scores):
            my_score = next(filter(lambda score: score[0] == name, scores))
            fox_score = next(filter(lambda score: score[4], scores))

            max_distance = sqrt(current_map.size_x**2 + current_map.size_y**2)

            def randomize(score):
                return score * 0.8 + 0.2 * random()

            def rate_score(current_score):
                is_fox = current_score == fox_score

                if is_fox:
                    min_distance = min([ distance(current_score, score) for score in scores if score != current_score])
                    return 1 - min_distance / max_distance
                return distance(current_score, fox_score) / max_distance

            return (
                randomize(rate_score(my_score)),
                randomize(sum([ rate_score(score) for score in scores if score != my_score ]))
            )

        def select_move(scores, n, last_rating):
            if abs(current_time() - start_time) > timeout:
                return 2, None
            if n == max(current_map.size_x, current_map.size_y) / 2:
                return 2, None

            best_rating = (+inf, -inf)
            best = None

            permutations = None
            if n < 5:
                permutations = itertools.permutations(possible_moves, len(scores))
            else:
                permutations = []
                for move in possible_moves:
                    permutation = ['n'] * len(scores)
                    permutation[my_index] = move
                    permutations.append(permutation)

            for moves in permutations:
                new_scores = []
                rating = 0

                my_move = None
                for score, move in zip(scores, moves):
                    delta = possible_moves[move]
                    if current_map.is_move_possible(score, delta):
                        x = score[2] + delta[0]
                        y = score[3] + delta[1]

                        if score[0] == name:
                            my_move = move

                        new_scores.append((score[0], score[1], x, y, score[4]))

                    else:
                        rating = -1
                        break

                if rating == -1:
                    continue

                rating = rate(new_scores)

                if rating == 0:
                    print('this shouldnot happen!')
                    return (rating, my_move)

                aborted = False
                if rating[0] > 0.95 * last_rating[0] or rating[1] < 0.75 * last_rating[1]:
                    bt_rating, bt_move = select_move(new_scores, n + 1, rating)

                    if bt_rating != 2:
                        rating = ((rating[0] + bt_rating[0]) / 2, (rating[1] + bt_rating[1]) / 2)

                    if bt_rating == 3:
                        aborted = True

                if best_rating[0] > rating[0]:
                    best_rating = rating
                    best = my_move

                if aborted:
                    return (best_rating, best)

            return (best_rating, best)

        rating, move = select_move(scores, 0, rate(scores))

        log("my choice", rating, move)

        connection.write(move)

    # terminate the loop if game is over
    elif str.startswith('game is over'):
        ongoing = False
