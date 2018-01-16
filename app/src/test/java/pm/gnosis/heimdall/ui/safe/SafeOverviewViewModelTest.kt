package pm.gnosis.heimdall.ui.safe

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.safe.overview.SafeOverviewViewModel
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import pm.gnosis.tests.utils.TestListUpdateCallback
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeOverviewViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var repositoryMock: GnosisSafeRepository

    private lateinit var viewModel: SafeOverviewViewModel

    @Before
    fun setup() {
        viewModel = SafeOverviewViewModel(repositoryMock)
    }

    @Test
    fun observeSafesResults() {
        val processor = PublishProcessor.create<List<AbstractSafe>>()
        val subscriber = createSubscriber()
        given(repositoryMock.observeSafes()).willReturn(processor)

        viewModel.observeSafes().subscribe(subscriber)

        then(repositoryMock).should().observeSafes()
        then(repositoryMock).shouldHaveNoMoreInteractions()
        // Check that the initial value is emitted
        subscriber.assertNoErrors()
                .assertValueCount(1)
                .assertValue { it is DataResult && it.data.parentId == null && it.data.diff == null && it.data.entries.isEmpty() }
        val initialDataId = (subscriber.values().first() as DataResult).data.id

        val results = listOf(Safe(BigInteger.ZERO), Safe(BigInteger.ONE))
        processor.offer(results)
        // Check that the results are emitted
        subscriber.assertNoErrors()
                .assertValueCount(2)
                .assertValueAt(1, { it is DataResult && it.data.parentId == initialDataId && it.data.diff != null && it.data.entries == results })

        val firstData = (subscriber.values()[1] as DataResult).data
        val firstDataId = firstData.id
        val callback = TestListUpdateCallback()
        callback.apply(firstData.diff!!)
                .assertNoChanges().assertNoRemoves().assertNoMoves()
                .assertInsertsCount(2).assertInserts(0, 2)
                .reset()

        val moreResults = listOf(Safe(BigInteger.ONE), Safe(BigInteger.ZERO), Safe(BigInteger.valueOf(3)))
        processor.offer(moreResults)
        // Check that the diff are calculated correctly
        subscriber.assertNoErrors()
                .assertValueCount(3)
                .assertValueAt(2, { it is DataResult && it.data.parentId == firstDataId && it.data.diff != null && it.data.entries == moreResults })

        val secondData = (subscriber.values()[2] as DataResult).data
        callback.apply(secondData.diff!!)
                .assertNoRemoves()
                // A remove might become a move (if it has an insert). So as "1" is "removed" it
                // stays at the end of the list. Once all the removes and inserts are done the moves
                // are calculated. Therefore it is a move from 2 to 0
                .assertMovesCount(1).assertMove(TestListUpdateCallback.Move(2, 0))
                .assertChangesCount(0)
                // Inserts are calculated from the back
                .assertInsertsCount(1).assertInsert(1)
                .reset()
    }

    @Test
    fun observeSafesError() {
        val subscriber = createSubscriber()
        val error = IllegalStateException()
        given(repositoryMock.observeSafes()).willReturn(Flowable.error(error))

        viewModel.observeSafes().subscribe(subscriber)

        then(repositoryMock).should().observeSafes()
        then(repositoryMock).shouldHaveNoMoreInteractions()
        // Check that the results are emitted
        subscriber.assertNoErrors()
                .assertValueCount(2)
                .assertValueAt(0, { it is DataResult && it.data.parentId == null && it.data.diff == null && it.data.entries.isEmpty() })
                .assertValueAt(1, { it is ErrorResult && it.error == error })
    }

    @Test
    fun removeSafeSuccess() {
        val observer = TestObserver.create<Unit>()
        val completable = TestCompletable()
        given(repositoryMock.remove(MockUtils.any())).willReturn(completable)

        viewModel.removeSafe(BigInteger.ZERO).subscribe(observer)

        then(repositoryMock).should().remove(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, completable.callCount)
        observer.assertTerminated().assertNoErrors().assertNoValues()
    }

    @Test
    fun removeSafeError() {
        val observer = TestObserver.create<Unit>()
        val error = IllegalStateException()
        given(repositoryMock.remove(MockUtils.any())).willReturn(Completable.error(error))

        viewModel.removeSafe(BigInteger.ZERO).subscribe(observer)

        then(repositoryMock).should().remove(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
        observer.assertTerminated().assertNoValues()
                .assertError(error)
    }

    @Test
    fun loadSafeInfoOnErrorLoadsFromCache() {
        val testObserver = TestObserver.create<SafeInfo>()
        val subject = PublishSubject.create<SafeInfo>()
        val safeInfo = SafeInfo("0x0", Wei(BigInteger.ZERO), 0, emptyList(), false)
        given(repositoryMock.loadInfo(MockUtils.any())).willReturn(subject)

        viewModel.loadSafeInfo(BigInteger.ZERO).subscribe(testObserver)
        subject.onNext(safeInfo)

        testObserver.assertValue(safeInfo)

        // Error loading the same safe (eg.: no internet) -> should load from cache
        val testObserver2 = TestObserver.create<SafeInfo>()
        viewModel.loadSafeInfo(BigInteger.ZERO).subscribe(testObserver2)
        subject.onError(Exception())

        testObserver2.assertResult(safeInfo)
        then(repositoryMock).should(times(2)).loadInfo(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadSafeInfoError() {
        val testObserver = TestObserver.create<SafeInfo>()
        val exception = Exception()
        given(repositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadSafeInfo(BigInteger.ZERO).subscribe(testObserver)

        then(repositoryMock).should().loadInfo(BigInteger.ZERO)
        then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun observeDeployedStatus() {
        val result = "result"
        val testObserver = TestObserver.create<String>()
        given(repositoryMock.observeDeployStatus(anyString())).willReturn(Observable.just(result))

        viewModel.observeDeployedStatus("test").subscribe(testObserver)

        then(repositoryMock).should().observeDeployStatus("test")
        then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(result)
    }

    @Test
    fun observeDeployedStatusError() {
        val exception = Exception()
        val testObserver = TestObserver.create<String>()
        given(repositoryMock.observeDeployStatus(anyString())).willReturn(Observable.error(exception))

        viewModel.observeDeployedStatus("test").subscribe(testObserver)

        then(repositoryMock).should().observeDeployStatus("test")
        then(repositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    private fun createSubscriber() = TestSubscriber.create<Result<Adapter.Data<AbstractSafe>>>()
}