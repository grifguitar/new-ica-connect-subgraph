import numpy as np
import csv

import matplotlib.pyplot as plt

from sklearn.decomposition import FastICA


def data_read(filename, out_x, out_y):
    x = list(list())
    y = list()
    with open(filename, 'r') as csvfile:
        rows = csv.reader(csvfile, delimiter='\t')
        for row in rows:
            y.append(row[0])
            x.append(row[1:])

    matrix = np.array(x).astype(float)
    labels = np.array(y).astype(str)
    np.savetxt(fname=out_x, X=matrix, fmt='%0.8f')
    np.savetxt(fname=out_y, X=labels, fmt='%s')

    return matrix


def solve():
    folder = '../real_data/'
    test_name = 'real_test_new'
    out_file = folder + test_name + '.fast_ica'

    mtx = data_read(folder + test_name + '.mtx', 'mtx.txt', 'mtx_labels.txt')

    ica = FastICA(n_components=2)

    modules = ica.fit_transform(mtx)

    np.savetxt(fname=out_file, X=modules, fmt='%0.10f', delimiter='\t')

    print(modules.shape)


def draw(data1, data2, name1, name2, fullname):
    fig, (ax1, ax2) = plt.subplots(nrows=2, sharex='all')
    fig.suptitle(fullname)
    plt.subplots_adjust(top=0.85, hspace=0.4)
    ax1.set_title(name1)
    ax1.boxplot(data1, vert=0)
    ax2.set_title(name2)
    ax2.boxplot(data2, vert=0)
    plt.savefig('../_answers_/' + name1 + '-' + name2 + '.png')
    plt.show()


def precalculate_data():
    data_nc025 = list()
    data_my025 = list()
    data_nc04 = list()
    data_my04 = list()
    data_nc05 = list()
    data_my05 = list()
    with open('../_answers_/agg3.txt', 'r') as csvfile:
        rows = csv.reader(csvfile, delimiter=',')
        for row in rows:
            if row[0].find('nc_0.25') != -1:
                data_nc025.append(float(row[1]))
            if row[0].find('x_0.25') != -1 or row[0].find('y_0.25') != -1:
                data_my025.append(float(row[1]))
            if row[0].find('nc_0.4') != -1:
                data_nc04.append(float(row[1]))
            if row[0].find('x_0.4') != -1 or row[0].find('y_0.4') != -1:
                data_my04.append(float(row[1]))
            if row[0].find('nc_0.5') != -1:
                data_nc05.append(float(row[1]))
            if row[0].find('x_0.5') != -1 or row[0].find('y_0.5') != -1:
                data_my05.append(float(row[1]))

    data_nc025 = np.array(data_nc025)
    data_my025 = np.array(data_my025)
    print(data_nc025.shape, " ", data_my025.shape)

    data_nc04 = np.array(data_nc04)
    data_my04 = np.array(data_my04)
    print(data_nc04.shape, " ", data_my04.shape)

    data_nc05 = np.array(data_nc05)
    data_my05 = np.array(data_my05)
    print(data_nc05.shape, " ", data_my05.shape)

    draw(data_nc025, data_my025, 'NetClust:0.25', 'MIQP-ICA', 'F1-score, noise 025, average 2 modules by 11 tests')
    draw(data_nc04, data_my04, 'NetClust:0.4', 'MIQP-ICA', 'F1-score, noise 025, average 2 modules by 11 tests')
    draw(data_nc05, data_my05, 'NetClust:0.5', 'MIQP-ICA', 'F1-score, noise 025, average 2 modules by 11 tests')


if __name__ == '__main__':
    solve()
    # precalculate_data()
