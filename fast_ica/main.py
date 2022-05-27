import numpy as np
import csv

import matplotlib.pyplot as plt

from sklearn.decomposition import FastICA

from scipy.stats import wilcoxon


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
    for i in range(40):
        folder = '../agg_1/'
        test_name = 'a' + str(i + 1) + '_test_1'
        out_file = folder + test_name + '.fast_ica'

        mtx = data_read(folder + test_name + '.mtx', 'mtx.txt', 'mtx_labels.txt')

        ica = FastICA(n_components=2)

        modules = ica.fit_transform(mtx)

        np.savetxt(fname=out_file, X=modules, fmt='%0.10f', delimiter='\t')

        print(modules.shape)


def precalculate_data():
    data_nc025 = list()
    data_my025 = list()
    data_nc04 = list()
    data_my04 = list()
    data_nc05 = list()
    data_my05 = list()

    roc_my = list()
    roc_fast_ica = list()

    with open('../_answers_/aggregate_1_20.txt', 'r') as csvfile:
        rows = csv.reader(csvfile, delimiter=',')
        for row in rows:
            if row[0].find('nc_0.25') != -1:
                data_nc025.append(float(row[1]))
            if row[0].find('x_0.25') != -1 or row[0].find('y_0.25') != -1:
                data_my025.append(float(row[1]))
                roc_my.append(float(row[2]))
                roc_fast_ica.append(float(row[3]))
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
    print(data_nc025)
    print(data_my025)
    print()

    data_nc04 = np.array(data_nc04)
    data_my04 = np.array(data_my04)
    print(data_nc04)
    print(data_my04)
    print()

    data_nc05 = np.array(data_nc05)
    data_my05 = np.array(data_my05)
    print(data_nc05)
    print(data_my05)
    print()

    my_dict1 = {
        'NetClust:0.25': data_nc025,
        'MIQP-ICA:0.25': data_my025,
        'NetClust:0.4': data_nc04,
        'MIQP-ICA:0.4': data_my04,
        'NetClust:0.5': data_nc05,
        'MIQP-ICA:0.5': data_my05,
    }

    my_dict2 = {
        'FastICA': roc_fast_ica,
        'MIQP-ICA': roc_my
    }

    fig, ax = plt.subplots()
    fig.suptitle('F1-score, noise 1, average 2 modules by 20 tests')
    fig.set_size_inches(9, 5)
    ax.boxplot(my_dict1.values(), vert=0)
    ax.set_yticklabels(my_dict1.keys())
    plt.savefig('../_answers_/picture_noise_1_f1score.png')
    plt.show()

    fig, ax = plt.subplots()
    fig.suptitle('AUC ROC, noise 1, average 2 modules by 20 tests')
    fig.set_size_inches(9, 5)
    ax.boxplot(my_dict2.values(), vert=0)
    ax.set_yticklabels(my_dict2.keys())
    plt.savefig('../_answers_/picture_noise_1_auc_roc.png')
    plt.show()

    print('0.25', wilcoxon(data_nc025, data_my025))

    print('0.4', wilcoxon(data_nc04, data_my04))

    print('0.5', wilcoxon(data_nc05, data_my05))

    print('AUC_ROC', wilcoxon(roc_fast_ica, roc_my))


if __name__ == '__main__':
    # solve()
    precalculate_data()
